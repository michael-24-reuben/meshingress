import { createElement, prettyJson } from "./utils.js";

const JSON_RPC_ERROR_NAMES = new Map([
  [-32700, "PARSE_ERROR"],
  [-32600, "INVALID_REQUEST"],
  [-32601, "METHOD_NOT_FOUND"],
  [-32602, "INVALID_PARAMS"],
  [-32603, "INTERNAL_ERROR"],
  [-32001, "UNAUTHORIZED"],
  [-32003, "FORBIDDEN"]
]);

export function errorNameForCode(code) {
  return JSON_RPC_ERROR_NAMES.get(Number(code)) || "JSON_RPC_ERROR";
}

export function normalizeClientError(error, request) {
  return {
    transportError: true,
    request,
    error: {
      code: "CLIENT_ERROR",
      name: "CLIENT_ERROR",
      message: error?.message || "Request failed",
      data: error?.stack || null
    }
  };
}

export function normalizeJsonRpcError(response, request) {
  const error = response?.error || {};
  return {
    response,
    request,
    error: {
      code: error.code,
      name: errorNameForCode(error.code),
      message: error.message || "JSON-RPC error",
      data: error.data
    }
  };
}

export function assertValidJsonRpcResponse(response) {
  if (!response || typeof response !== "object") {
    throw new Error("Response body is not a JSON object.");
  }
  if (response.error === undefined && response.result === undefined) {
    throw new Error("Response does not include result or error.");
  }
}

export function renderErrorDetails(errorInfo) {
  const container = createElement("div", { className: "response-content" });
  const message = createElement("div", { className: "content-block" });
  message.append(
    createElement("strong", { text: `${errorInfo.error.name || "ERROR"} ` }),
    createElement("span", { text: `${errorInfo.error.code ?? ""}` }),
    createElement("p", { text: errorInfo.error.message || "Unknown error" })
  );
  container.append(message);

  if (errorInfo.error.data !== undefined && errorInfo.error.data !== null) {
    const data = createElement("pre", { className: "content-block", text: prettyJson(errorInfo.error.data) });
    container.append(data);
  }

  return container;
}
