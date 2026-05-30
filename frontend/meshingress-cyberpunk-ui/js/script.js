import { normalizeClientError, normalizeJsonRpcError, assertValidJsonRpcResponse } from "./errorHandler.js";
import { appendResponse } from "./responseRenderer.js";
import { buildRequestHeaders, getSettings, onSettingsChanged, resetSettings, updateSettings } from "./settings.js";
import { createToolRefreshController, fetchToolList } from "./toolListFetcher.js";
import { buildCallPayload, collectFormArguments, renderToolForm } from "./toolFormGenerator.js";
import { createElement, formatTimestamp, generateJsonRpcId, prettyJson, replaceChildren } from "./utils.js";

const highRiskScopes = new Set([
  "SHELL_EXECUTE",
  "FILES_WRITE",
  "FILES_DELETE",
  "NETWORK_ACCESS",
  "HTTP_CLIENT",
  "WEBSOCKET_CONNECT",
  "EXTERNAL_API_WRITE",
  "PROCESS_EXECUTE",
  "PROCESS_KILL",
  "SECRETS_READ",
  "TOKEN_ISSUE",
  "TOKEN_REVOKE",
  "POLICY_WRITE",
  "TOOLS_CALL"
]);

const state = {
  tools: [],
  selectedTool: null,
  submitting: false,
  suppressRiskPrompt: false
};

const elements = {
  endpointLabel: document.querySelector("#endpointLabel"),
  toolCountLabel: document.querySelector("#toolCountLabel"),
  lastRefreshLabel: document.querySelector("#lastRefreshLabel"),
  toolStatus: document.querySelector("#toolStatus"),
  toolSelect: document.querySelector("#toolSelect"),
  toolDetails: document.querySelector("#toolDetails"),
  toolCallForm: document.querySelector("#toolCallForm"),
  formFields: document.querySelector("#formFields"),
  submitButton: document.querySelector("#submitButton"),
  resetFormButton: document.querySelector("#resetFormButton"),
  schemaView: document.querySelector("#schemaView"),
  annotationsView: document.querySelector("#annotationsView"),
  payloadPreview: document.querySelector("#payloadPreview"),
  responseHistory: document.querySelector("#responseHistory"),
  clearHistoryButton: document.querySelector("#clearHistoryButton"),
  refreshToolsButton: document.querySelector("#refreshToolsButton"),
  settingsButton: document.querySelector("#settingsButton"),
  settingsDialog: document.querySelector("#settingsDialog"),
  settingsForm: document.querySelector("#settingsForm"),
  settingsMount: document.querySelector("#settingsMount"),
  resetSettingsButton: document.querySelector("#resetSettingsButton"),
  riskDialog: document.querySelector("#riskDialog"),
  riskForm: document.querySelector("#riskForm"),
  riskMessage: document.querySelector("#riskMessage"),
  riskDoNotAsk: document.querySelector("#riskDoNotAsk"),
  operationStatusCard: document.querySelector("#operationStatusCard"),
  operationStateLabel: document.querySelector("#operationStateLabel"),
  processNarrative: document.querySelector("#processNarrative"),
  processRail: document.querySelector("#processRail"),
  activityLog: document.querySelector("#activityLog")
};

const refreshController = createToolRefreshController(() => refreshTools());

bootstrap();

async function bootstrap() {
  await loadSettingsTemplate();
  bindEvents();
  renderSettings(getSettings());
  updateEndpointLabels();
  refreshController.start(getSettings().refreshIntervalMs);
  await refreshTools();
}

function bindEvents() {
  elements.toolSelect.addEventListener("change", () => {
    selectTool(elements.toolSelect.value);
  });

  elements.toolCallForm.addEventListener("input", updatePayloadPreview);
  elements.toolCallForm.addEventListener("submit", (event) => {
    event.preventDefault();
    submitToolCall();
  });

  elements.resetFormButton.addEventListener("click", () => {
    if (state.selectedTool) {
      renderToolForm(elements.formFields, state.selectedTool);
      updatePayloadPreview();
    }
  });

  elements.refreshToolsButton.addEventListener("click", () => refreshTools({ manual: true }));
  elements.clearHistoryButton.addEventListener("click", () => {
    replaceChildren(elements.responseHistory, [
      createElement("div", { className: "empty-state large-empty", text: "No tool calls yet. Submit a request to open the return channel." })
    ]);
    logActivity("Response timeline cleared.");
    setProcessState("idle", "Response channel reset. Awaiting next request.");
  });

  elements.settingsButton.addEventListener("click", () => {
    renderSettings(getSettings());
    elements.settingsDialog.showModal();
  });

  elements.settingsForm.addEventListener("submit", (event) => {
    if (event.submitter?.value === "cancel") {
      return;
    }
    event.preventDefault();
    applySettingsFromForm();
    elements.settingsDialog.close();
  });

  elements.resetSettingsButton.addEventListener("click", () => {
    renderSettings(resetSettings());
    refreshTools();
  });

  document.querySelectorAll("[data-inspector-tab]").forEach((button) => {
    button.addEventListener("click", () => selectInspectorTab(button.dataset.inspectorTab));
  });

  onSettingsChanged((settings) => {
    updateEndpointLabels(settings);
    refreshController.start(settings.refreshIntervalMs);
  });
}

async function loadSettingsTemplate() {
  const fallback = `
    <div class="settings-grid">
      <label class="field-group"><span>MCP endpoint URL</span><input name="endpointUrl" type="url" required></label>
      <label class="field-group"><span>Tool refresh interval, milliseconds</span><input name="refreshIntervalMs" type="number" min="5000" step="1000" required></label>
      <label class="field-group"><span>Bearer token, memory only</span><input name="bearerToken" type="password" autocomplete="off" placeholder="Optional"></label>
      <label class="field-group"><span>MCP session ID, memory only</span><input name="sessionId" type="text" autocomplete="off" placeholder="Optional"></label>
    </div>`;
  try {
    const response = await fetch("./settings.html");
    elements.settingsMount.innerHTML = response.ok ? await response.text() : fallback;
  } catch {
    elements.settingsMount.innerHTML = fallback;
  }
}

async function refreshTools(options = {}) {
  elements.toolStatus.textContent = "Synchronizing tool mesh...";
  elements.refreshToolsButton.classList.add("loading");
  setProcessState("refreshing", options.manual ? "Manual refresh requested. Pulling tool registry state." : "Automatic registry sync in progress.", ["refresh"]);
  logActivity(options.manual ? "Manual tool registry refresh started." : "Tool registry refresh started.");
  try {
    const result = await fetchToolList(getSettings());
    state.tools = result.tools;
    populateTools(result.tools);
    elements.toolCountLabel.textContent = `${result.tools.length} loaded`;
    elements.lastRefreshLabel.textContent = formatTimestamp(result.fetchedAt);
    elements.toolStatus.textContent = result.tools.length
      ? "Tools loaded from the configured MCP endpoint."
      : "The endpoint returned no public tools.";
    setProcessState("success", `${result.tools.length} tool${result.tools.length === 1 ? "" : "s"} synchronized. Select a target and compose a payload.`, ["refresh", "compose"]);
    logActivity(`Refresh complete: ${result.tools.length} tools available.`);
  } catch (error) {
    elements.toolStatus.textContent = error.message;
    elements.toolCountLabel.textContent = "Refresh failed";
    setProcessState("error", `Registry refresh failed: ${error.message}`, ["refresh"]);
    logActivity(`Refresh failed: ${error.message}`);
  } finally {
    elements.refreshToolsButton.classList.remove("loading");
  }
}

function populateTools(tools) {
  const options = [];
  if (tools.length === 0) {
    options.push(createElement("option", { text: "No tools available", attributes: { value: "" } }));
  }
  for (const tool of tools) {
    const label = tool.title ? `${tool.name} - ${tool.title}` : tool.name;
    options.push(createElement("option", { text: label, attributes: { value: tool.name } }));
  }
  replaceChildren(elements.toolSelect, options);
  selectTool(tools[0]?.name || "");
}

function selectTool(toolName) {
  state.selectedTool = state.tools.find((tool) => tool.name === toolName) || null;
  elements.submitButton.disabled = !state.selectedTool;
  renderSelectedTool();
  updatePayloadPreview();
  if (state.selectedTool) {
    setProcessState("idle", `${state.selectedTool.name} selected. Payload preview is live.`, ["refresh", "compose"]);
    logActivity(`Selected tool: ${state.selectedTool.name}`);
  }
}

function renderSelectedTool() {
  const tool = state.selectedTool;
  if (!tool) {
    elements.schemaView.textContent = "";
    elements.annotationsView.textContent = "";
    replaceChildren(elements.formFields, []);
    elements.toolDetails.textContent = "Select a tool to inspect its schema and annotations.";
    return;
  }

  const scopes = getToolScopes(tool);
  const risky = scopes.filter((scope) => highRiskScopes.has(scope));
  const details = createElement("div", { className: "tool-details" });
  details.append(
    createElement("div", { className: "tool-title", text: tool.title || tool.name }),
    createElement("div", { text: tool.description || "No description provided." }),
    renderScopeBadges(scopes, risky)
  );
  replaceChildren(elements.toolDetails, [details]);
  renderToolForm(elements.formFields, tool);
  elements.schemaView.textContent = prettyJson(tool.inputSchema || {});
  elements.annotationsView.textContent = prettyJson(tool.annotations || {});
}

function renderScopeBadges(scopes, risky) {
  const row = createElement("div", { className: "badge-row" });
  if (scopes.length === 0) {
    row.append(createElement("span", { className: "badge", text: "No scopes declared" }));
    return row;
  }
  for (const scope of scopes) {
    row.append(createElement("span", {
      className: `badge${risky.includes(scope) ? " danger" : ""}`,
      text: scope
    }));
  }
  return row;
}

async function submitToolCall() {
  if (!state.selectedTool || state.submitting) {
    return;
  }

  try {
    if (!(await confirmRiskIfNeeded(state.selectedTool))) {
      return;
    }

    state.submitting = true;
    elements.submitButton.disabled = true;
    elements.submitButton.textContent = "Transmitting...";

    setProcessState("sending", `Composing ${state.selectedTool.name} request payload.`, ["compose"]);
    const requestId = generateJsonRpcId("tool-call");
    const argumentsValue = collectFormArguments(elements.toolCallForm, state.selectedTool);
    const payload = buildCallPayload(state.selectedTool, argumentsValue, requestId);
    logActivity(`Payload composed: ${requestId}`);
    setProcessState("sending", `Dispatching ${requestId} to MCP endpoint.`, ["compose", "send"]);
    const settings = getSettings();
    const response = await fetch(settings.endpointUrl, {
      method: "POST",
      headers: buildRequestHeaders(settings, requestId),
      body: JSON.stringify(payload)
    });
    setProcessState("sending", `HTTP response received. Decoding JSON-RPC envelope.`, ["compose", "send", "receive"]);
    const body = await response.json();
    assertValidJsonRpcResponse(body);

    if (body.error) {
      appendResponse(elements.responseHistory, normalizeJsonRpcError(body, payload));
      setProcessState("error", body.error.message || "JSON-RPC error returned by endpoint.", ["compose", "send", "receive", "render"]);
      logActivity(`JSON-RPC error: ${body.error.message || body.error.code || "unknown"}`);
      return;
    }

    setProcessState("success", "Tool response decoded. Rendering content blocks and metadata tabs.", ["compose", "send", "receive", "render"]);
    logActivity(`Tool call completed: ${requestId}`);
    appendResponse(elements.responseHistory, {
      at: new Date(),
      toolName: state.selectedTool.name,
      request: payload,
      response: body
    });
  } catch (error) {
    appendResponse(elements.responseHistory, normalizeClientError(error, buildCurrentPayloadPreview()));
    setProcessState("error", error.message || "Client-side request failed.", ["compose", "send", "receive", "render"]);
    logActivity(`Request failed: ${error.message || "unknown error"}`);
  } finally {
    state.submitting = false;
    elements.submitButton.disabled = !state.selectedTool;
    elements.submitButton.textContent = "Submit Call";
  }
}

async function confirmRiskIfNeeded(tool) {
  const risky = getToolScopes(tool).filter((scope) => highRiskScopes.has(scope));
  if (risky.length === 0 || state.suppressRiskPrompt) {
    return true;
  }

  // #architect: High-risk scope confirmation is advisory in MVP, not a hard policy gate.
  // "Do not ask again" is memory-only and resets on reload.
  elements.riskMessage.textContent = `${tool.name} declares high-risk scopes: ${risky.join(", ")}. Review the generated arguments before proceeding.`;
  elements.riskDoNotAsk.checked = false;
  elements.riskDialog.showModal();
  const result = await new Promise((resolve) => {
    elements.riskForm.addEventListener("submit", (event) => {
      resolve(event.submitter?.value || "cancel");
    }, { once: true });
  });
  if (result === "proceed" && elements.riskDoNotAsk.checked) {
    state.suppressRiskPrompt = true;
  }
  return result === "proceed";
}

function setProcessState(stateName, narrative, activeSteps = []) {
  const normalized = stateName || "idle";
  elements.operationStatusCard?.classList.remove("idle", "refreshing", "sending", "success", "error", "active");
  elements.operationStatusCard?.classList.add(normalized);
  if (normalized === "refreshing" || normalized === "sending") {
    elements.operationStatusCard?.classList.add("active");
  }
  if (elements.operationStateLabel) {
    elements.operationStateLabel.textContent = labelForOperationState(normalized);
  }
  if (elements.processNarrative && narrative) {
    elements.processNarrative.textContent = narrative;
  }

  const steps = new Set(activeSteps);
  document.querySelectorAll("[data-process-step]").forEach((node) => {
    node.classList.remove("idle", "active", "sending", "success", "error");
    if (normalized === "error" && steps.has(node.dataset.processStep)) {
      node.classList.add("error");
    } else if (normalized === "success" && steps.has(node.dataset.processStep)) {
      node.classList.add("success");
    } else if ((normalized === "refreshing" || normalized === "sending") && steps.has(node.dataset.processStep)) {
      node.classList.add(normalized === "sending" ? "sending" : "active");
    } else if (steps.has(node.dataset.processStep)) {
      node.classList.add("success");
    } else {
      node.classList.add("idle");
    }
  });
}

function pulseStep(stepName) {
  const node = document.querySelector(`[data-process-step="${CSS.escape(stepName)}"]`);
  if (!node) {
    return;
  }
  node.classList.add("active");
  window.setTimeout(() => {
    if (!state.submitting) {
      node.classList.remove("active");
      node.classList.add("idle");
    }
  }, 240);
}

function labelForOperationState(stateName) {
  return {
    idle: "Idle",
    refreshing: "Refreshing",
    sending: "HTTP in flight",
    success: "Success",
    error: "Failure"
  }[stateName] || "Idle";
}

function logActivity(message) {
  if (!elements.activityLog || !message) {
    return;
  }
  const entry = createElement("div", {
    className: "activity-entry",
    text: `[${formatTimestamp(new Date())}] ${message}`
  });
  elements.activityLog.prepend(entry);
  while (elements.activityLog.children.length > 8) {
    elements.activityLog.lastElementChild?.remove();
  }
}

function updatePayloadPreview() {
  elements.payloadPreview.textContent = prettyJson(buildCurrentPayloadPreview());
  if (state.selectedTool && !state.submitting) {
    pulseStep("compose");
  }
}

function buildCurrentPayloadPreview() {
  if (!state.selectedTool) {
    return {};
  }
  try {
    const args = collectFormArguments(elements.toolCallForm, state.selectedTool, { validateRequired: false });
    return buildCallPayload(state.selectedTool, args, "GENERATED_ID");
  } catch (error) {
    return { error: error.message };
  }
}

function getToolScopes(tool) {
  const scopes = tool?.annotations?.scopes;
  return Array.isArray(scopes) ? scopes.map(String) : [];
}

function renderSettings(settings) {
  const form = elements.settingsForm;
  for (const [name, value] of Object.entries(settings)) {
    const field = form.elements.namedItem(name);
    if (field) {
      field.value = value;
    }
  }
}

function applySettingsFromForm() {
  const form = elements.settingsForm;
  updateSettings({
    endpointUrl: form.elements.endpointUrl.value,
    refreshIntervalMs: form.elements.refreshIntervalMs.value,
    bearerToken: form.elements.bearerToken.value,
    sessionId: form.elements.sessionId.value
  });
  refreshTools();
}

function updateEndpointLabels(settings = getSettings()) {
  elements.endpointLabel.textContent = settings.endpointUrl;
}

function selectInspectorTab(tabName) {
  document.querySelectorAll("[data-inspector-tab]").forEach((button) => {
    button.classList.toggle("active", button.dataset.inspectorTab === tabName);
  });
  document.querySelectorAll(".inspector-view").forEach((view) => {
    view.classList.add("hidden");
  });
  const target = {
    schema: elements.schemaView,
    annotations: elements.annotationsView,
    payload: elements.payloadPreview
  }[tabName];
  target?.classList.remove("hidden");
}
