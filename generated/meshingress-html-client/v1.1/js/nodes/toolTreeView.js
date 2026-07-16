import { createElement, replaceChildren } from "../utils.js";

export class ToolTreeView {
    constructor(options = {}) {
        this.root = createRootNode();
        this.tools = [];
        this.mount = options.mount || null;
        this.onSelect = options.onSelect || (() => {});
        this.onPreview = options.onPreview || (() => {});
        this.onPreviewEnd = options.onPreviewEnd || (() => {});
        this.selectedToolName = "";
        this.expandedPaths = new Set();
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
        this.tools.push(tool);
        this.tools.sort(compareTools);
        this.root = buildTree(this.tools);
        this.render();
        return true;
    }

    containsToolFunction(tool) {
        const name = toolName(tool);
        return Boolean(name) && this.tools.some((entry) => entry.name === name);
    }

    removeToolFunction(tool) {
        const name = toolName(tool);
        const before = this.tools.length;
        this.tools = this.tools.filter((entry) => entry.name !== name);
        if (this.selectedToolName === name) {
            this.selectedToolName = "";
        }
        this.root = buildTree(this.tools);
        this.render();
        return this.tools.length !== before;
    }

    remap(root) {
        this.tools = normalizeTools(root);
        this.root = buildTree(this.tools);
        this.render();
    }

    size() {
        return this.tools.length;
    }

    setSelected(toolNameValue) {
        this.selectedToolName = String(toolNameValue || "");
        this.render();
    }

    expandedState() {
        return new Set(this.expandedPaths);
    }

    restoreExpandedState(paths) {
        this.expandedPaths = new Set(paths || []);
        this.render();
    }

    render() {
        if (!this.mount) {
            return;
        }

        if (this.tools.length === 0) {
            replaceChildren(this.mount, [
                createElement("div", { className: "empty-state", text: "No tools available." })
            ]);
            return;
        }

        const container = createElement("div", { className: "tool-tree" });
        for (const child of Object.values(this.root.children)) {
            container.append(renderTreeNode(this, child));
        }
        replaceChildren(this.mount, [container]);
    }
}

function renderTreeNode(view, node) {
    const hasChildren = Object.keys(node.children).length > 0;
    if (!hasChildren && node.tool) {
        return createToolButton(view, node.tool);
    }

    const details = createElement("details", { className: "tool-tree-group" });
    details.open = view.expandedPaths.has(node.path) || view.expandedPaths.size === 0;
    details.addEventListener("toggle", () => {
        if (details.open) {
            view.expandedPaths.add(node.path);
        } else {
            view.expandedPaths.delete(node.path);
        }
    });

    const summary = createElement("summary", { className: "tool-tree-summary", text:  "📁 " + node.segment });
    const children = createElement("div", { className: "tool-tree-children" });

    if (node.tool) {
        children.append(createToolButton(view, node.tool));
    }

    for (const child of Object.values(node.children)) {
        children.append(renderTreeNode(view, child));
    }

    details.append(summary, children);
    return details;
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
        createElement("span", { className: "tool-node-title", text: tool.title || lastNamePart(tool.name) }),
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

function buildTree(tools) {
    const root = createRootNode();
    for (const tool of tools) {
        const parts = String(tool.name || "").split(".").filter(Boolean);
        if (parts.length === 0) {
            continue;
        }
        let current = root;
        let path = "";
        for (const part of parts) {
            path = path ? `${path}.${part}` : part;
            if (!current.children[part]) {
                current.children[part] = {
                    segment: part,
                    path,
                    children: {},
                    tool: null
                };
            }
            current = current.children[part];
        }
        current.tool = tool;
    }
    return root;
}

function createRootNode() {
    return {
        segment: "root",
        path: "",
        children: {},
        tool: null
    };
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

function lastNamePart(name) {
    const parts = String(name || "").split(".").filter(Boolean);
    return parts.at(-1) || name;
}
