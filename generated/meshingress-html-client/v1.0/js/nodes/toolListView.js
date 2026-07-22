import { createElement, replaceChildren } from "../utils.js";

export class ToolListView {
    constructor(options = {}) {
        this.root = [];
        this.mount = options.mount || null;
        this.onSelect = options.onSelect || (() => {});
        this.onPreview = options.onPreview || (() => {});
        this.onPreviewEnd = options.onPreviewEnd || (() => {});
        this.selectedToolName = "";
    }

    attach(mount) {
        this.mount = mount;
        this.render();
    }

    detach() {
        if (this.mount) {
            replaceChildren(this.mount, []);
        }
        this.mount = null;
    }

    addToolFunction(tool) {
        if (!isValidTool(tool) || this.containsToolFunction(tool)) {
            return false;
        }
        this.root.push(tool);
        this.root.sort(compareTools);
        this.render();
        return true;
    }

    containsToolFunction(tool) {
        const name = toolName(tool);
        return Boolean(name) && this.root.some((entry) => entry.name === name);
    }

    removeToolFunction(tool) {
        const name = toolName(tool);
        const before = this.root.length;
        this.root = this.root.filter((entry) => entry.name !== name);
        if (this.selectedToolName === name) {
            this.selectedToolName = "";
        }
        this.render();
        return this.root.length !== before;
    }

    remap(root) {
        this.root = normalizeTools(root);
        this.render();
    }

    size() {
        return this.root.length;
    }

    setSelected(toolNameValue) {
        this.selectedToolName = String(toolNameValue || "");
        this.render();
    }

    render() {
        if (!this.mount) {
            return;
        }

        if (this.root.length === 0) {
            replaceChildren(this.mount, [
                createElement("div", { className: "empty-state", text: "No tools available." })
            ]);
            return;
        }

        const container = createElement("div", { className: "tool-list" });
        for (const tool of this.root) {
            container.append(createToolButton(this, tool));
        }
        replaceChildren(this.mount, [container]);
    }
}

function createToolButton(view, tool) {
    const scopes = getToolScopes(tool);
    const button = createElement("button", {
        className: `tool-function-node${view.selectedToolName === tool.name ? " selected" : ""}`,
        attributes: {
            id: functionNodeId(tool.name),
            type: "button",
            "data-tool-name": tool.name,
            "data-scopes": scopes.join(",")
        }
    });

    const main = createElement("span", { className: "tool-node-main" });
    main.append(
        createElement("span", { className: "tool-node-title", text: tool.title || tool.name }),
        createElement("span", { className: "tool-node-name", text: tool.name })
    );
    button.append(main, createElement("span", { className: "tool-node-count", text: String(scopes.length) }));

    button.addEventListener("click", () => view.onSelect(tool));
    button.addEventListener("mouseenter", (event) => view.onPreview(tool, event));
    button.addEventListener("focus", (event) => view.onPreview(tool, event));
    button.addEventListener("mouseleave", () => view.onPreviewEnd(tool));
    button.addEventListener("blur", () => view.onPreviewEnd(tool));

    return button;
}

function normalizeTools(value) {
    if (!Array.isArray(value)) {
        return [];
    }
    return value.filter(isValidTool).slice().sort(compareTools);
}

function isValidTool(tool) {
    return Boolean(tool && typeof tool.name === "string" && tool.name.trim());
}

function compareTools(left, right) {
    return left.name.localeCompare(right.name);
}

function toolName(tool) {
    return typeof tool === "string" ? tool : tool?.name;
}

function getToolScopes(tool) {
    const scopes = tool?.annotations?.scopes;
    return Array.isArray(scopes) ? scopes.map(String) : [];
}

function functionNodeId(name) {
    return `fid-${String(name || "unknown").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "")}`;
}
