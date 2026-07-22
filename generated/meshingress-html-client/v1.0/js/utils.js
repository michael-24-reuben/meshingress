export function generateJsonRpcId(prefix = "mcp") {
    if (crypto?.randomUUID) {
        return `${prefix}-${crypto.randomUUID()}`;
    }
    return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function formatTimestamp(date = new Date()) {
    return new Intl.DateTimeFormat(undefined, {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        fractionalSecondDigits: 3
    }).format(date);
}

export function prettyJson(value) {
    return JSON.stringify(value ?? null, null, 2);
}

export function safeText(value) {
    if (value === null || value === undefined) {
        return "";
    }
    return typeof value === "string" ? value : prettyJson(value);
}

export function parseJsonTextarea(value, fieldName) {
    const trimmed = value.trim();
    if (!trimmed) {
        return undefined;
    }
    try {
        return JSON.parse(trimmed);
    } catch (error) {
        throw new Error(`${fieldName} must contain valid JSON: ${error.message}`);
    }
}

export function createElement(tagName, options = {}) {
    const element = document.createElement(tagName);
    if (options.className) {
        element.className = options.className;
    }
    if (options.text !== undefined) {
        element.textContent = options.text;
    }
    if (options.attributes) {
        for (const [name, value] of Object.entries(options.attributes)) {
            if (value !== undefined && value !== null) {
                element.setAttribute(name, String(value));
            }
        }
    }
    return element;
}

export function createToolNodeElement(tagName, options = {}) {
    const element = document.createElement(tagName);
    if (options.className) {
        element.className = options.className;
    }
    if (options.text !== undefined) {
        element.textContent = options.text;
    }
    if (options.attributes) {
        for (const [name, value] of Object.entries(options.attributes)) {
            if (value !== undefined && value !== null) {
                element.setAttribute(name, String(value));
            }
        }
    }
    return element;
}


export function replaceChildren(parent, children) {
    parent.replaceChildren(...children.filter(Boolean));
}

export function sanitizeHtml(html) {
    const parser = new DOMParser();
    const documentValue = parser.parseFromString(String(html ?? ""), "text/html");
    const blockedTags = new Set(["script", "iframe", "object", "embed", "link", "meta", "base"]);
    const walker = documentValue.createTreeWalker(documentValue.body, NodeFilter.SHOW_ELEMENT);
    const nodes = [];
    while (walker.nextNode()) {
        nodes.push(walker.currentNode);
    }

    for (const node of nodes) {
        if (blockedTags.has(node.tagName.toLowerCase())) {
            node.remove();
            continue;
        }
        for (const attribute of [...node.attributes]) {
            const name = attribute.name.toLowerCase();
            const value = attribute.value.trim().toLowerCase();
            if (name.startsWith("on") || value.startsWith("javascript:")) {
                node.removeAttribute(attribute.name);
            }
        }
    }

    return documentValue.body.innerHTML;
}

export function isPlainObject(value) {
    return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}
