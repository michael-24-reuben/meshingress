export const DEFAULT_SETTINGS = Object.freeze({
  endpointUrl: "http://100.121.15.11:4737/mcp",
  refreshIntervalMs: 30000,
  bearerToken: "",
  sessionId: ""
});

let currentSettings = { ...DEFAULT_SETTINGS };
const listeners = new Set();

// #architect: Settings are memory-only in MVP. Future transport/settings work should design
// localStorage persistence after the security and auth model is finalized.
export function getSettings() {
  return { ...currentSettings };
}

export function updateSettings(nextSettings) {
  const normalized = normalizeSettings({ ...currentSettings, ...nextSettings });
  currentSettings = normalized;
  for (const listener of listeners) {
    listener(getSettings());
  }
  return getSettings();
}

export function resetSettings() {
  currentSettings = { ...DEFAULT_SETTINGS };
  for (const listener of listeners) {
    listener(getSettings());
  }
  return getSettings();
}

export function onSettingsChanged(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function buildRequestHeaders(settings, requestId) {
  const headers = {
    "Accept": "application/json",
    "Content-Type": "application/json",
    "X-Request-Id": requestId
  };

  // #architect: Authentication is intentionally a no-op shell in MVP.
  // Do not store auth tokens until Meshingress authentication is implemented.
  if (settings.bearerToken) {
    headers.Authorization = `Bearer ${settings.bearerToken}`;
  }

  if (settings.sessionId) {
    headers["X-Mcp-Session-Id"] = settings.sessionId;
  }

  return headers;
}

function normalizeSettings(value) {
  const refreshIntervalMs = Number(value.refreshIntervalMs);
  return {
    endpointUrl: String(value.endpointUrl || DEFAULT_SETTINGS.endpointUrl).trim(),
    refreshIntervalMs: Number.isFinite(refreshIntervalMs)
      ? Math.max(5000, Math.trunc(refreshIntervalMs))
      : DEFAULT_SETTINGS.refreshIntervalMs,
    bearerToken: String(value.bearerToken || "").trim(),
    sessionId: String(value.sessionId || "").trim()
  };
}
