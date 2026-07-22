import fs from "node:fs";
import path from "node:path";

const startedAt = new Date();
let timestamp = startedAt.toISOString();
const downloadCallId = "solo-leveling-0-202";
const publicationCallPrefix = "publication-status-";
let publicationPoll = null;
let publicationWorkspace = null;
let lastPublicationState = null;

console.log("> Tool call started at ", timestamp);

const socket = new WebSocket("ws://100.121.15.11:4737/mcp/ws");

socket.addEventListener("open", () => {
    socket.send(JSON.stringify({
        "jsonrpc": "2.0",
        "id": downloadCallId,
        "method": "tools/call",
        "params": {
            "name": "toonverse.download-book",
            "arguments": {
                "name": "solo leveling",
                "minChapterNumber": 0,
                "maxChapterNumber": 5,
                "ttlSeconds": 86400,
                "maxRequests": 50000
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

        const download = result.structuredContent;
        console.log(`[completed] ${download.chapterCount} chapters, ${download.pageCount} pages | ${elapsedSeconds}s`);
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
        const suffix = status.lastError ? ` | ${status.lastError}` : "";
        console.log(`[publication] ${status.state} | attempts ${status.attempts ?? 0}${suffix}`);
        lastPublicationState = status.state;
    }
    if (["HANDED_OFF", "AVAILABLE", "HANDOFF_FAILED", "FAILED"].includes(status.state)) {
        if (publicationPoll) clearInterval(publicationPoll);
        socket.close(1000, `Publication ${status.state}`);
    }
}

