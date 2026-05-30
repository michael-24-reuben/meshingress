import { buildRequestHeaders } from "./settings.js";
import { generateJsonRpcId } from "./utils.js";
import { assertValidJsonRpcResponse } from "./errorHandler.js";

export async function fetchToolList(settings) {
  const requestId = generateJsonRpcId("tools-list");
  const payload = {
    jsonrpc: "2.0",
    id: requestId,
    method: "tools/list",
    params: {}
  };

  const response = await fetch(settings.endpointUrl, {
    method: "POST",
    headers: buildRequestHeaders(settings, requestId),
    body: JSON.stringify(payload)
  });

  const body = await parseJsonResponse(response);
  assertValidJsonRpcResponse(body);
  if (body.error) {
    const message = body.error.message || "tools/list failed";
    throw new Error(`${message} (${body.error.code ?? "unknown"})`);
  }

  const tools = Array.isArray(body.result?.tools) ? body.result.tools : [];
  return {
    tools: tools.slice().sort((left, right) => left.name.localeCompare(right.name)),
    raw: body,
    fetchedAt: new Date()
  };
}

export function createToolRefreshController(fetcher) {
  let timerId = null;

  return {
    start(intervalMs) {
      this.stop();
      if (intervalMs > 0) {
        timerId = window.setInterval(fetcher, intervalMs);
      }
    },
    stop() {
      if (timerId !== null) {
        window.clearInterval(timerId);
        timerId = null;
      }
    }
  };
}

async function parseJsonResponse(response) {
  const text = await response.text();
  try {
    return text ? JSON.parse(text) : {};
  } catch (error) {
    throw new Error(`Invalid JSON response from ${response.url || "endpoint"}: ${error.message}`);
  }
}
