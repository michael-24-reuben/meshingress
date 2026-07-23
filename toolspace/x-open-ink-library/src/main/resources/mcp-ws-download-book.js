import fs from "node:fs";
import path from "node:path";

const socketUrl = process.env.MESHINGRESS_MCP_WS_URL ?? "ws://100.121.15.11:4737/mcp/ws";
const bookName = process.env.MESHINGRESS_BOOK_NAME ?? "solo leveling";
const minChapterNumber = integerEnvironment("MESHINGRESS_MIN_CHAPTER", 0);
const maxChapterNumber = integerEnvironment("MESHINGRESS_MAX_CHAPTER", 3);
const ttlSeconds = integerEnvironment("MESHINGRESS_TTL_SECONDS", 86400);
const maxRequests = integerEnvironment("MESHINGRESS_MAX_REQUESTS", 50000);
const monitorSeconds = integerEnvironment("MESHINGRESS_MONITOR_SECONDS", 900);
const terminalPublicationStates = new Set(["COMPLETED", "FAILED", "AVAILABLE", "HANDED_OFF", "HANDOFF_FAILED"]);
const startedAt = new Date();
let timestamp = startedAt.toISOString();
const downloadCallId = `download-${timestamp.replace(/[:.]/g, "-")}`;
const publicationCallPrefix = "publication-status-";
let publicationPoll = null;
let publicationDeadline = null;
let publicationWorkspace = null;
let lastPublicationState = null;

if (maxChapterNumber < minChapterNumber) {
    throw new Error("MESHINGRESS_MAX_CHAPTER must be greater than or equal to MESHINGRESS_MIN_CHAPTER.");
}

console.log(`> Delegated tool call started at ${timestamp} | ${bookName} chapters ${minChapterNumber}-${maxChapterNumber}`);

const socket = new WebSocket(socketUrl);

socket.addEventListener("open", () => {
    socket.send(JSON.stringify({
        "jsonrpc": "2.0",
        "id": downloadCallId,
        "method": "tools/call",
        "params": {
            "name": "toonverse.download-book",
            "arguments": {
                "name": bookName,
                "minChapterNumber": minChapterNumber,
                "maxChapterNumber": maxChapterNumber,
                "ttlSeconds": ttlSeconds,
                "maxRequests": maxRequests
            }
        }
    }));
});

// const fs = require("node:fs");
// const path = require("node:path");

timestamp = timestamp
    .replace(/:/g, "-")
    .replace(/\.\d{3}Z$/, "");

const logDirectory = path.join("data", "tools", "openinklibrary", "logs");
fs.mkdirSync(logDirectory, {recursive: true});

const logFile = path.join(logDirectory, `tool-call-${timestamp}.jsonl`);
const errFile = path.join(logDirectory, `std-error-${timestamp}.jsonl`);

socket.addEventListener("message", event => {
    const response = JSON.parse(event.data);
    const jsonLine = JSON.stringify(response);

    printResponse(response);
    fs.appendFileSync(logFile, `${jsonLine}\n`, "utf8");

    if (response.id === downloadCallId) {
        beginPublicationMonitoring(response);
        return;
    }

    if (typeof response.id === "string" && response.id.startsWith(publicationCallPrefix)) {
        reportPublicationStatus(response);
    }
});

socket.addEventListener("error", error => {
    console.error("WebSocket error:", error);
    fs.appendFileSync(errFile, `${JSON.stringify(error)}\n`, "utf8");
});

socket.addEventListener("close", event => {
    if (publicationPoll) clearInterval(publicationPoll);
    if (publicationDeadline) clearTimeout(publicationDeadline);
    console.log(`[socket] closed ${event.code}${event.reason ? ` | ${event.reason}` : ""}`);
});

function printResponse(response) {
    if (response.method === "notifications/progress") {
        const progress = response.params.progress;
        console.log(`[progress] ${progress.state} ${progress.unitsCompleted}/${progress.total} | ${progress.message}`);
        return;
    }

    if (response.method === "notifications/progress/estimate") {
        const estimate = response.params.estimate;
        console.log(`[estimate]  ${estimate.unitsCompleted}/${estimate.total} | remaining ${estimate.estimatedRemaining} | ${estimate.observedSecondsPerUnit.toFixed(2)}s/chapter`);
        return;
    }

    if (response.id === downloadCallId) {
        const elapsedSeconds = ((Date.now() - startedAt.getTime()) / 1000).toFixed(2);
        const result = response.result;
        if (result.isError) {
            console.error(`[failed]  ${result._meta.errorCode}: ${result._meta.errorMessage} | ${elapsedSeconds}s`);
            return;
        }

        const download = result.structuredContent ?? {};
        console.log(`[accepted] ${download.chapterCount ?? 0} chapters, ${download.pageCount ?? 0} pages | ${elapsedSeconds}s`);
    }
}

function beginPublicationMonitoring(response) {
    if (response.result?.isError) {
        socket.close(1000, "Download failed");
        return;
    }

    const workspace = response.result?.structuredContent?.workspace;
    if (!workspace?.sessionId || !workspace?.requestId) {
        console.error("[publication] The download response did not contain a workspace identifier.");
        socket.close(1011, "Missing workspace identifier");
        return;
    }

    publicationWorkspace = workspace;
    console.log(`[publication] ${workspace.publicationState ?? "UNKNOWN"} | monitoring ${workspace.sessionId}/${workspace.requestId}`);
    pollPublicationStatus();
    publicationPoll = setInterval(pollPublicationStatus, 1_000);
    publicationDeadline = setTimeout(() => {
        console.error(`[publication] monitoring timed out after ${monitorSeconds}s.`);
        socket.close(1011, "Publication monitor timeout");
    }, monitorSeconds * 1_000);
}

function pollPublicationStatus() {
    if (!publicationWorkspace || socket.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify({
        "jsonrpc": "2.0",
        "id": `${publicationCallPrefix}${Date.now()}`,
        "method": "tools/call",
        "params": {
            "name": "toonverse.publication-status",
            "arguments": {
                "sessionId": publicationWorkspace.sessionId,
                "requestId": publicationWorkspace.requestId
            }
        }
    }));
}

function reportPublicationStatus(response) {
    if (response.result?.isError) {
        console.error(`[publication] status request failed: ${response.result?._meta?.errorMessage ?? "unknown error"}`);
        return;
    }
    const status = response.result?.structuredContent;
    if (!status?.state) {
        console.error("[publication] status response was malformed.");
        return;
    }
    if (status.state !== lastPublicationState) {
        const error = status.lastError ?? status.error;
        const suffix = error ? ` | ${error}` : "";
        console.log(`[publication] ${status.state} | attempts ${status.attempts ?? 0}${suffix}`);
        lastPublicationState = status.state;
    }
    if (terminalPublicationStates.has(status.state)) {
        if (publicationPoll) clearInterval(publicationPoll);
        if (publicationDeadline) clearTimeout(publicationDeadline);
        socket.close(1000, `Publication ${status.state}`);
    }
}

function integerEnvironment(name, fallback) {
    const value = process.env[name];
    if (value === undefined || value === "") return fallback;
    if (!/^-?\d+$/.test(value)) throw new Error(`${name} must be an integer.`);
    return Number.parseInt(value, 10);
}
