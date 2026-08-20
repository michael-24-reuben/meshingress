import fs from "node:fs";
import path from "node:path";

const socketUrl = process.env.MESHINGRESS_MCP_WS_URL ?? "ws://100.121.15.11:4737/mcp/ws";
const bookName = process.env.MESHINGRESS_BOOK_NAME ?? "solo leveling";
const minChapterNumber = integerEnvironment("MESHINGRESS_MIN_CHAPTER", 0);
const maxChapterNumber = integerEnvironment("MESHINGRESS_MAX_CHAPTER", 3);
const ttlSeconds = integerEnvironment("MESHINGRESS_TTL_SECONDS", 86400);
const maxRequests = integerEnvironment("MESHINGRESS_MAX_REQUESTS", 50000);
const monitorSeconds = integerEnvironment("MESHINGRESS_MONITOR_SECONDS", 900);
const resumeSessionId = process.env.MESHINGRESS_SESSION_ID?.trim() ?? "";
const resumeRequestId = process.env.MESHINGRESS_REQUEST_ID?.trim() ?? "";
const reconnectDelayMilliseconds = 1_000;
const terminalPublicationStates = new Set(["COMPLETED", "FAILED", "AVAILABLE", "HANDED_OFF", "HANDOFF_FAILED"]);
const startedAt = new Date();
let timestamp = startedAt.toISOString();
const downloadCallId = `download-${timestamp.replace(/[:.]/g, "-")}`;
const publicationCallPrefix = "publication-status-";
const logDirectory = path.join("data", "tools", "openinklibrary", "logs");
const logFile = path.join(logDirectory, `tool-call-${timestamp.replace(/:/g, "-").replace(/\.\d{3}Z$/, "")}.jsonl`);
const errFile = path.join(logDirectory, `std-error-${timestamp.replace(/:/g, "-").replace(/\.\d{3}Z$/, "")}.jsonl`);

let socket = null;
let publicationPoll = null;
let publicationDeadline = null;
let reconnectTimer = null;
let publicationWorkspace = null;
let lastPublicationState = null;
let terminal = false;
let reconnects = 0;

if (maxChapterNumber < minChapterNumber) {
    throw new Error("MESHINGRESS_MAX_CHAPTER must be greater than or equal to MESHINGRESS_MIN_CHAPTER.");
}
if (Boolean(resumeSessionId) !== Boolean(resumeRequestId)) {
    throw new Error("MESHINGRESS_SESSION_ID and MESHINGRESS_REQUEST_ID must be supplied together.");
}

fs.mkdirSync(logDirectory, {recursive: true});
console.log(`> Delegated tool call started at ${timestamp} | ${bookName} chapters ${minChapterNumber}-${maxChapterNumber}`);
if (resumeSessionId) {
    publicationWorkspace = {sessionId: resumeSessionId, requestId: resumeRequestId, publicationState: "RESUMING"};
    console.log(`[publication] resuming ${resumeSessionId}/${resumeRequestId}`);
    startPublicationMonitoring();
}
connect();

function connect() {
    socket = new WebSocket(socketUrl);
    socket.addEventListener("open", () => {
        if (publicationWorkspace) {
            console.log(`[socket] reconnected ${reconnects} time(s); resuming publication polling.`);
            pollPublicationStatus();
            return;
        }
        socket.send(JSON.stringify({
            jsonrpc: "2.0",
            id: downloadCallId,
            method: "tools/call",
            params: {
                name: "open-ink-library.toonverse.download-book",
                arguments: {name: bookName, minChapterNumber, maxChapterNumber, ttlSeconds, maxRequests},
            },
        }));
    });

    socket.addEventListener("message", event => {
        let response;
        try {
            response = JSON.parse(event.data);
        } catch (error) {
            recordError("WebSocket message was not JSON", error);
            return;
        }
        fs.appendFileSync(logFile, `${JSON.stringify(response)}\n`, "utf8");
        printResponse(response);
        if (response.id === downloadCallId) {
            beginPublicationMonitoring(response);
        } else if (typeof response.id === "string" && response.id.startsWith(publicationCallPrefix)) {
            reportPublicationStatus(response);
        }
    });

    socket.addEventListener("error", error => recordError("WebSocket error", error));
    socket.addEventListener("close", event => {
        console.log(`[socket] closed ${event.code}${event.reason ? ` | ${event.reason}` : ""}`);
        if (!terminal && publicationWorkspace) {
            reconnects += 1;
            reconnectTimer = setTimeout(connect, reconnectDelayMilliseconds);
        }
    });
}

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
        if (response.result?.isError) {
            console.error(`[failed] ${response.result._meta?.errorCode}: ${response.result._meta?.errorMessage} | ${elapsedSeconds}s`);
            terminal = true;
            return;
        }
        const download = response.result?.structuredContent ?? {};
        console.log(`[accepted] ${download.chapterCount ?? 0} chapters, ${download.pageCount ?? 0} pages | ${elapsedSeconds}s`);
    }
}

function beginPublicationMonitoring(response) {
    if (publicationWorkspace || response.result?.isError) {
        if (response.result?.isError && socket?.readyState === WebSocket.OPEN) socket.close(1000, "Download failed");
        return;
    }
    const workspace = response.result?.structuredContent?.workspace;
    if (!workspace?.sessionId || !workspace?.requestId) {
        terminal = true;
        console.error("[publication] The download response did not contain a workspace identifier.");
        socket.close(1011, "Missing workspace identifier");
        return;
    }
    publicationWorkspace = workspace;
    console.log(`[publication] ${workspace.publicationState ?? "UNKNOWN"} | monitoring ${workspace.sessionId}/${workspace.requestId}`);
    startPublicationMonitoring();
}

function startPublicationMonitoring() {
    pollPublicationStatus();
    publicationPoll = setInterval(pollPublicationStatus, 1_000);
    publicationDeadline = setTimeout(() => {
        terminal = true;
        clearInterval(publicationPoll);
        console.error(`[publication] monitoring timed out after ${monitorSeconds}s.`);
        socket?.close(1011, "Publication monitor timeout");
    }, monitorSeconds * 1_000);
}

function pollPublicationStatus() {
    if (!publicationWorkspace || socket?.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify({
        jsonrpc: "2.0",
        id: `${publicationCallPrefix}${Date.now()}`,
        method: "storage/publication-status",
        params: {sessionId: publicationWorkspace.sessionId, requestId: publicationWorkspace.requestId},
    }));
}

function reportPublicationStatus(response) {
    if (response.error) {
        console.error(`[publication] status request failed: ${response.error.message ?? "unknown error"}`);
        return;
    }
    const status = response.result;
    if (!status?.state) {
        console.error("[publication] status response was malformed.");
        return;
    }
    if (status.state !== lastPublicationState) {
        const error = status.lastError ?? status.error;
        console.log(`[publication] ${status.state} | attempts ${status.attempts ?? 0}${error ? ` | ${error}` : ""}`);
        lastPublicationState = status.state;
    }
    if (terminalPublicationStates.has(status.state)) {
        terminal = true;
        clearInterval(publicationPoll);
        clearTimeout(publicationDeadline);
        if (reconnectTimer) clearTimeout(reconnectTimer);
        socket?.close(1000, `Publication ${status.state}`);
    }
}

function recordError(prefix, error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`${prefix}: ${message}`);
    fs.appendFileSync(errFile, `${JSON.stringify({at: new Date().toISOString(), prefix, message})}\n`, "utf8");
}

function integerEnvironment(name, fallback) {
    const value = process.env[name];
    if (value === undefined || value === "") return fallback;
    if (!/^-?\d+$/.test(value)) throw new Error(`${name} must be an integer.`);
    return Number.parseInt(value, 10);
}
