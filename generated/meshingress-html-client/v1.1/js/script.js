import { normalizeClientError, normalizeJsonRpcError, assertValidJsonRpcResponse } from "./errorHandler.js";
import { appendResponse } from "./responseRenderer.js";
import { buildRequestHeaders, getSettings, onSettingsChanged, resetSettings, updateSettings } from "./settings.js";
import { createToolRefreshController, fetchToolList } from "./toolListFetcher.js";
import { buildCallPayload, collectFormArguments, renderToolForm } from "./toolFormGenerator.js";
import { createElement, formatTimestamp, generateJsonRpcId, prettyJson, replaceChildren } from "./utils.js";
import { ToolTreeView } from "./nodes/toolTreeView.js";
import { ToolListView } from "./nodes/toolListView.js";

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
    suppressRiskPrompt: false,
    firstLoadComplete: false,
    ui: {
        selectedToolName: "",
        inspectorCollapsed: false,
        inspectorActiveTab: "schema",
        toolNavigationMode: "tree",
        toolNavigationScrollTop: 0
    }
};

const elements = {
    endpointLabel: document.querySelector("#endpointLabel"),
    toolCountLabel: document.querySelector("#toolCountLabel"),
    lastRefreshLabel: document.querySelector("#lastRefreshLabel"),
    selectedToolLabel: document.querySelector("#selectedToolLabel"),
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
    toolNavigationMount: document.querySelector("#toolNavigationMount"),
    toolTreePanel: document.querySelector("#toolTreePanel"),
    toolNodeHoverCard: document.querySelector("#toolNodeHoverCard"),
    inspectorPanel: document.querySelector("#inspectorPanel"),
    inspectorToggleButton: document.querySelector("#inspectorToggleButton"),
    appShell: document.querySelector(".app-shell")
};

const toolTreeView = new ToolTreeView({
    onSelect: (tool) => selectTool(tool.name),
    onPreview: showToolNodePreview,
    onPreviewEnd: hideToolNodePreview
});

const toolListView = new ToolListView({
    onSelect: (tool) => selectTool(tool.name),
    onPreview: showToolNodePreview,
    onPreviewEnd: hideToolNodePreview
});

const refreshController = createToolRefreshController(() => refreshTools());

bootstrap();

async function bootstrap() {
    await loadSettingsTemplate();
    bindEvents();
    renderSettings(getSettings());
    updateEndpointLabels();
    applyInspectorState();
    renderToolNavigation();
    refreshController.start(getSettings().refreshIntervalMs);
    await refreshTools();
}

function bindEvents() {
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

    elements.refreshToolsButton.addEventListener("click", () => refreshTools());
    elements.clearHistoryButton.addEventListener("click", () => {
        replaceChildren(elements.responseHistory, [
            createElement("div", { className: "empty-state", text: "No tool calls yet." })
        ]);
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

    document.querySelectorAll("[data-tool-nav-mode]").forEach((button) => {
        button.addEventListener("click", () => selectToolNavigationMode(button.dataset.toolNavMode));
    });

    elements.toolNavigationMount.addEventListener("scroll", () => {
        state.ui.toolNavigationScrollTop = elements.toolNavigationMount.scrollTop;
    });

    elements.inspectorToggleButton.addEventListener("click", toggleInspector);
    elements.inspectorPanel.querySelector(".inspector-collapsed-strip")?.addEventListener("click", toggleInspector);

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

async function refreshTools() {
    captureUiState();
    try {
        const previousSelectedToolName = state.ui.selectedToolName;
        const result = await fetchToolList(getSettings());
        state.tools = result.tools;

        elements.toolCountLabel.textContent = `${result.tools.length} loaded`;
        elements.lastRefreshLabel.textContent = formatTimestamp(result.fetchedAt);

        if (result.tools.length === 0) {
            state.firstLoadComplete = true;
            clearToolSurface("No tools available", "The configured MCP endpoint responded successfully, but it returned no public tools.");
            return;
        }

        toolTreeView.remap(result.tools);
        toolListView.remap(result.tools);
        renderToolNavigation();

        const nextSelectedToolName = resolveSelectedToolName(result.tools, previousSelectedToolName);
        selectTool(nextSelectedToolName, { forceRender: !state.firstLoadComplete || nextSelectedToolName !== previousSelectedToolName });
        state.firstLoadComplete = true;
        restoreNavigationScroll();
    } catch (error) {
        state.tools = [];
        elements.toolCountLabel.textContent = "Refresh failed";
        elements.lastRefreshLabel.textContent = formatTimestamp(new Date());
        clearToolSurface("Host cannot be reached", error?.message || "The configured MCP endpoint did not respond.");
    }
}

function resolveSelectedToolName(tools, previousSelectedToolName) {
    if (previousSelectedToolName && tools.some((tool) => tool.name === previousSelectedToolName)) {
        return previousSelectedToolName;
    }
    return tools[0]?.name || "";
}

function clearToolSurface(title, detail = "") {
    state.selectedTool = null;
    state.ui.selectedToolName = "";
    hideToolNodePreview();
    toolTreeView.remap([]);
    toolListView.remap([]);
    toolTreeView.setSelected("");
    toolListView.setSelected("");
    replaceChildren(elements.toolNavigationMount, []);
    renderCenterMessage(title, detail);
    replaceChildren(elements.formFields, []);
    elements.submitButton.disabled = true;
    elements.selectedToolLabel.textContent = "None";
    elements.schemaView.textContent = "";
    elements.annotationsView.textContent = "";
    elements.payloadPreview.textContent = "";
    replaceChildren(elements.responseHistory, [
        createElement("div", { className: "empty-state", text: "No tool calls yet." })
    ]);
}

function renderCenterMessage(title, detail = "") {
    const children = [createElement("h2", { text: title, attributes: { id: "toolDetailsTitle" } })];
    if (detail) {
        children.push(createElement("p", { text: detail }));
    }
    replaceChildren(elements.toolDetails, children);
    elements.toolDetails.classList.add("empty-state", "tool-message-state");
}


function selectTool(toolName, options = {}) {
    const previousToolName = state.ui.selectedToolName;
    state.selectedTool = state.tools.find((tool) => tool.name === toolName) || null;
    state.ui.selectedToolName = state.selectedTool?.name || "";
    elements.submitButton.disabled = !state.selectedTool;
    elements.selectedToolLabel.textContent = state.ui.selectedToolName || "None";
    toolTreeView.setSelected(state.ui.selectedToolName);
    toolListView.setSelected(state.ui.selectedToolName);

    if (options.forceRender === false && previousToolName === state.ui.selectedToolName) {
        updateSelectedToolDetailsOnly();
        updatePayloadPreview();
        return;
    }

    renderSelectedTool();
    updatePayloadPreview();
}

function renderSelectedTool() {
    const tool = state.selectedTool;
    if (!tool) {
        renderCenterMessage("No tool selected", "Select a tool from the sidebar.");
        elements.schemaView.textContent = "";
        elements.annotationsView.textContent = "";
        elements.payloadPreview.textContent = "";
        replaceChildren(elements.formFields, []);
        elements.submitButton.disabled = true;
        elements.selectedToolLabel.textContent = "None";
        return;
    }

    updateSelectedToolDetailsOnly();
    renderToolForm(elements.formFields, tool);
}

function updateSelectedToolDetailsOnly() {
    const tool = state.selectedTool;
    if (!tool) {
        return;
    }
    const scopes = getToolScopes(tool);
    const risky = scopes.filter((scope) => highRiskScopes.has(scope));
    replaceChildren(elements.toolDetails, [
        createElement("h2", { className: "tool-title", text: tool.title || tool.name, attributes: { id: "toolDetailsTitle" } }),
        createElement("div", { className: "tool-name-line", text: tool.name }),
        createElement("div", { className: "tool-description", text: tool.description || "No description provided." }),
        renderScopeBadges(scopes, risky)
    ]);
    elements.toolDetails.classList.remove("empty-state", "tool-message-state");
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
        elements.submitButton.textContent = "Submitting...";

        const requestId = generateJsonRpcId("tool-call");
        const argumentsValue = collectFormArguments(elements.toolCallForm, state.selectedTool);
        const payload = buildCallPayload(state.selectedTool, argumentsValue, requestId);
        const settings = getSettings();
        const response = await fetch(settings.endpointUrl, {
            method: "POST",
            headers: buildRequestHeaders(settings, requestId),
            body: JSON.stringify(payload)
        });
        const body = await response.json();
        assertValidJsonRpcResponse(body);

        if (body.error) {
            appendResponse(elements.responseHistory, normalizeJsonRpcError(body, payload));
            return;
        }

        appendResponse(elements.responseHistory, {
            at: new Date(),
            toolName: state.selectedTool.name,
            request: payload,
            response: body
        });
    } catch (error) {
        appendResponse(elements.responseHistory, normalizeClientError(error, buildCurrentPayloadPreview()));
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

function updatePayloadPreview() {
    elements.payloadPreview.textContent = prettyJson(buildCurrentPayloadPreview());
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
    state.ui.inspectorActiveTab = tabName || "schema";
    document.querySelectorAll("[data-inspector-tab]").forEach((button) => {
        button.classList.toggle("active", button.dataset.inspectorTab === state.ui.inspectorActiveTab);
    });
    document.querySelectorAll(".inspector-view").forEach((view) => {
        view.classList.add("hidden");
    });
    const target = {
        schema: elements.schemaView,
        annotations: elements.annotationsView,
        payload: elements.payloadPreview
    }[state.ui.inspectorActiveTab];
    target?.classList.remove("hidden");
}

function toggleInspector() {
    state.ui.inspectorCollapsed = !state.ui.inspectorCollapsed;
    applyInspectorState();
}

function applyInspectorState() {
    elements.inspectorPanel.classList.toggle("collapsed", state.ui.inspectorCollapsed);
    elements.appShell.classList.toggle("inspector-collapsed", state.ui.inspectorCollapsed);
    elements.inspectorToggleButton.setAttribute("aria-expanded", String(!state.ui.inspectorCollapsed));
    elements.inspectorToggleButton.setAttribute(
        "aria-label",
        state.ui.inspectorCollapsed ? "Expand inspector" : "Collapse inspector"
    );
    elements.inspectorToggleButton.textContent = state.ui.inspectorCollapsed ? "‹" : "›";
    selectInspectorTab(state.ui.inspectorActiveTab);
}

function selectToolNavigationMode(mode) {
    captureUiState();
    state.ui.toolNavigationMode = mode === "list" ? "list" : "tree";
    renderToolNavigation();
}

function renderToolNavigation() {
    document.querySelectorAll("[data-tool-nav-mode]").forEach((button) => {
        button.classList.toggle("active", button.dataset.toolNavMode === state.ui.toolNavigationMode);
    });

    if (state.ui.toolNavigationMode === "list") {
        toolTreeView.detach();
        toolListView.attach(elements.toolNavigationMount);
    } else {
        toolListView.detach();
        toolTreeView.attach(elements.toolNavigationMount);
    }

    restoreNavigationScroll();
}

function captureUiState() {
    state.ui.toolNavigationScrollTop = elements.toolNavigationMount?.scrollTop || 0;
}

function restoreNavigationScroll() {
    requestAnimationFrame(() => {
        if (elements.toolNavigationMount) {
            elements.toolNavigationMount.scrollTop = state.ui.toolNavigationScrollTop;
        }
    });
}

function showToolNodePreview(tool, event) {
    const card = elements.toolNodeHoverCard;
    const scopes = getToolScopes(tool);
    const title = createElement("div", { className: "tool-hover-title", text: tool.title || tool.name });
    const name = createElement("div", { className: "tool-hover-name", text: tool.name });
    const description = createElement("div", {
        className: "tool-hover-description",
        text: tool.description || "No description provided."
    });
    const scopeRow = createElement("div", { className: "scope-icon-row" });

    if (scopes.length === 0) {
        scopeRow.append(createElement("span", { className: "tool-hover-description", text: "No scopes declared" }));
    } else {
        for (const scope of scopes) {
            scopeRow.append(createScopeIcon(scope));
        }
    }

    replaceChildren(card, [title, name, description, scopeRow]);
    card.style.visibility = "hidden";
    card.classList.remove("hidden");
    positionHoverCard(card, event.currentTarget);
    card.style.visibility = "visible";
}

function hideToolNodePreview() {
    elements.toolNodeHoverCard.style.visibility = "";
    elements.toolNodeHoverCard.classList.add("hidden");
}

function createScopeIcon(scope) {
    const chip = createElement("span", {
        className: "scope-icon-chip",
        attributes: { title: scope, "aria-label": scope }
    });
    const img = createElement("img", {
        attributes: {
            src: `./assets/scopes/${scopeAssetName(scope)}.svg`,
            alt: ""
        }
    });
    img.addEventListener("error", () => {
        img.src = "./assets/scopes/unknown-scope.svg";
    }, { once: true });
    chip.append(img);
    return chip;
}

function scopeAssetName(scope) {
    return String(scope || "unknown-scope")
        .toLowerCase()
        .replace(/_/g, "-")
        .replace(/[^a-z0-9-]+/g, "-")
        .replace(/^-+|-+$/g, "") || "unknown-scope";
}

function positionHoverCard(card, anchor) {
    const rect = anchor.getBoundingClientRect();
    const gap = 12;
    const projectedLeft = rect.right + gap;
    const maxLeft = window.innerWidth - card.offsetWidth - 16;
    const left = Math.max(16, Math.min(projectedLeft, maxLeft));
    const maxTop = window.innerHeight - card.offsetHeight - 16;
    const top = Math.max(16, Math.min(rect.top, maxTop));
    card.style.left = `${left}px`;
    card.style.top = `${top}px`;
}
