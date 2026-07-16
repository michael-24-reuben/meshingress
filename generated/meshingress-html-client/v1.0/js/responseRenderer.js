import { createElement, formatTimestamp, prettyJson, safeText, sanitizeHtml } from "./utils.js";
import { renderErrorDetails } from "./errorHandler.js";

export function appendResponse(historyContainer, entry) {
  const empty = historyContainer.querySelector(".empty-state");
  if (empty) {
    empty.remove();
  }
  historyContainer.prepend(renderResponseCard(entry));
}

export function renderResponseCard(entry) {
  const response = entry.response;
  const isTransportError = Boolean(entry.error);
  const isToolError = Boolean(response?.result?.isError);
  const card = createElement("article", {
    className: `response-card${isTransportError ? " error" : ""}${isToolError ? " tool-error" : ""}`
  });

  const title = isTransportError
    ? "Request failed"
    : isToolError ? "Tool returned an error result" : "Tool call completed";

  const header = createElement("div", { className: "response-card-header" });
  header.append(
    createElement("div", { className: "response-card-title", text: title }),
    createElement("div", {
      className: "response-meta",
      text: `${entry.toolName || "unknown"} | ${formatTimestamp(entry.at)} | ${entry.request?.id || "no id"}`
    })
  );
  card.append(header);

  if (isTransportError) {
    card.append(renderErrorDetails(entry));
    return card;
  }

  const tabs = renderTabs([
    ["rendered", "Rendered"],
    ["structured", "Structured"],
    ["raw", "Raw"],
    ["meta", "Meta"]
  ]);
  const views = createElement("div", { className: "response-content" });
  const renderedView = renderRenderedContent(response?.result);
  renderedView.dataset.responseView = "rendered";
  const structuredView = renderJsonBlock(response?.result?.structuredContent ?? null);
  structuredView.dataset.responseView = "structured";
  structuredView.classList.add("hidden");
  const rawView = renderJsonBlock(response);
  rawView.dataset.responseView = "raw";
  rawView.classList.add("hidden");
  const metaView = renderJsonBlock(response?.result?._meta ?? {});
  metaView.dataset.responseView = "meta";
  metaView.classList.add("hidden");
  views.append(renderedView, structuredView, rawView, metaView);
  bindTabs(tabs, views);

  card.append(tabs, views);
  return card;
}

export function renderRenderedContent(result) {
  const container = createElement("div", { className: "response-content" });
  const content = Array.isArray(result?.content) ? result.content : [];
  if (content.length === 0) {
    container.append(createElement("div", { className: "empty-state", text: "No rendered content returned." }));
    return container;
  }

  for (const item of content) {
    container.append(renderContentItem(item));
  }
  return container;
}

function renderContentItem(item) {
  const mimeType = item?.mimeType || "";
  if (item?.type === "text") {
    const block = createElement("div", { className: "content-block" });
    block.append(createElement("pre", { text: safeText(item.text) }));
    return block;
  }
  if (item?.type === "json" || mimeType.includes("application/json")) {
    return renderJsonBlock(item.data ?? item.text ?? item);
  }
  if (mimeType.includes("text/html")) {
    const iframe = createElement("iframe", {
      className: "html-preview",
      attributes: { sandbox: "", title: "Sanitized HTML preview" }
    });
    iframe.srcdoc = sanitizeHtml(contentValue(item));
    const block = createElement("div", { className: "content-block" });
    block.append(iframe);
    return block;
  }
  if (mimeType.startsWith("image/")) {
    return renderMediaBlock("img", item, "image-preview");
  }
  if (mimeType.startsWith("audio/")) {
    return renderMediaBlock("audio", item, "", { controls: "controls" });
  }
  return renderJsonBlock(item);
}

function renderMediaBlock(tagName, item, className, attributes = {}) {
  const src = typeof item.data === "string" ? item.data : item.data?.src || item.data?.url;
  if (!src || !isSafeMediaSource(src)) {
    return renderJsonBlock(item);
  }
  const media = createElement(tagName, {
    className,
    attributes: { ...attributes, src }
  });
  const block = createElement("div", { className: "content-block" });
  block.append(media);
  return block;
}

function renderJsonBlock(value) {
  const block = createElement("pre", {
    className: "content-block",
    text: prettyJson(value)
  });
  return block;
}

function renderTabs(tabs) {
  const container = createElement("div", { className: "tabs", attributes: { role: "tablist" } });
  for (const [id, label] of tabs) {
    container.append(createElement("button", {
      className: `tab${id === "rendered" ? " active" : ""}`,
      text: label,
      attributes: { type: "button", "data-response-tab": id }
    }));
  }
  return container;
}

function bindTabs(tabs, views) {
  tabs.addEventListener("click", (event) => {
    const button = event.target.closest("[data-response-tab]");
    if (!button) {
      return;
    }
    const selected = button.dataset.responseTab;
    for (const tab of tabs.querySelectorAll("[data-response-tab]")) {
      tab.classList.toggle("active", tab === button);
    }
    for (const view of views.querySelectorAll("[data-response-view]")) {
      view.classList.toggle("hidden", view.dataset.responseView !== selected);
    }
  });
}

function contentValue(item) {
  if (typeof item.data === "string") {
    return item.data;
  }
  if (typeof item.text === "string") {
    return item.text;
  }
  return prettyJson(item.data ?? item);
}

function isSafeMediaSource(value) {
  return value.startsWith("data:") || value.startsWith("https://") || value.startsWith("http://");
}
