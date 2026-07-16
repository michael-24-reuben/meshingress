import { createElement, isPlainObject, parseJsonTextarea, prettyJson, replaceChildren } from "./utils.js";

const FIELD_TYPES = new Set(["string", "integer", "number", "boolean", "array", "object"]);

export function renderToolForm(container, tool) {
  const schema = tool?.inputSchema;
  if (!isPlainObject(schema) || schema.type !== "object" || !isPlainObject(schema.properties)) {
    renderRawJsonFallback(container, schema);
    return;
  }

  const required = new Set(Array.isArray(schema.required) ? schema.required : []);
  const fields = Object.entries(schema.properties).map(([name, propertySchema]) => {
    return createField(name, propertySchema || {}, required.has(name));
  });

  if (fields.length === 0) {
    replaceChildren(container, [
      createElement("div", { className: "empty-state", text: "This tool does not require arguments." })
    ]);
    return;
  }

  replaceChildren(container, fields);
}

export function collectFormArguments(form, tool, options = {}) {
  const validateRequired = options.validateRequired !== false;
  const schema = tool?.inputSchema;
  if (!isPlainObject(schema) || schema.type !== "object" || !isPlainObject(schema.properties)) {
    const raw = form.querySelector("[data-raw-json]");
    return parseJsonTextarea(raw?.value || "{}", "arguments") || {};
  }

  const required = new Set(Array.isArray(schema.required) ? schema.required : []);
  const argumentsValue = {};
  for (const [name, propertySchema] of Object.entries(schema.properties)) {
    const field = form.querySelector(`[name="${CSS.escape(name)}"]`);
    const value = readFieldValue(field, propertySchema || {}, required.has(name), name, validateRequired);
    if (value !== undefined) {
      argumentsValue[name] = value;
    }
  }
  return argumentsValue;
}

export function buildCallPayload(tool, argumentsValue, id) {
  return {
    jsonrpc: "2.0",
    id,
    method: "tools/call",
    params: {
      name: tool.name,
      arguments: argumentsValue
    }
  };
}

function createField(name, schema, required) {
  const type = schemaType(schema);
  if (!FIELD_TYPES.has(type) && !Array.isArray(schema.enum)) {
    return createRawField(name, schema, required);
  }

  const wrapper = createElement("div", { className: "field-group generated-field" });
  const label = createElement("label", { attributes: { for: `field-${name}` } });
  label.append(createElement("span", { text: name }));
  if (required) {
    label.append(createElement("span", { className: "required-mark", text: " *" }));
  }

  const input = createInput(name, type, schema, required);
  const helper = schema.description
    ? createElement("small", { text: schema.description })
    : null;
  const error = createElement("div", {
    className: "field-error hidden",
    attributes: { "data-field-error": name }
  });

  wrapper.append(label, input);
  if (helper) {
    wrapper.append(helper);
  }
  wrapper.append(error);
  return wrapper;
}

function createInput(name, type, schema, required) {
  if (Array.isArray(schema.enum)) {
    const select = createElement("select", {
      attributes: { id: `field-${name}`, name, "data-field-type": "enum" }
    });
    if (!required) {
      select.append(createElement("option", { text: "", attributes: { value: "" } }));
    }
    for (const value of schema.enum) {
      select.append(createElement("option", { text: String(value), attributes: { value: String(value) } }));
    }
    return select;
  }

  if (type === "boolean") {
    const label = createElement("label", { className: "checkbox-row" });
    const input = createElement("input", {
      attributes: { id: `field-${name}`, name, type: "checkbox", "data-field-type": type }
    });
    label.append(input, createElement("span", { text: "Enabled" }));
    return label;
  }

  if (type === "array" || type === "object") {
    return createElement("textarea", {
      text: type === "array" ? "[]" : "{}",
      attributes: { id: `field-${name}`, name, "data-field-type": type, required: required ? "required" : null }
    });
  }

  const inputType = type === "number" || type === "integer" ? "number" : "text";
  const attributes = { id: `field-${name}`, name, type: inputType, "data-field-type": type };
  if (required) {
    attributes.required = "required";
  }
  if (type === "integer") {
    attributes.step = "1";
  }
  return createElement("input", { attributes });
}

function createRawField(name, schema, required) {
  const wrapper = createElement("div", { className: "field-group generated-field" });
  const label = createElement("label", { text: `${name}${required ? " *" : ""}` });
  const textarea = createElement("textarea", {
    text: schema.default === undefined ? "" : prettyJson(schema.default),
    attributes: { name, "data-field-type": "raw", required: required ? "required" : null }
  });
  wrapper.append(
    label,
    textarea,
    createElement("small", { text: schema.description || "Unsupported schema keyword. Enter a JSON value." }),
    createElement("div", { className: "field-error hidden", attributes: { "data-field-error": name } })
  );
  return wrapper;
}

function renderRawJsonFallback(container, schema) {
  const wrapper = createElement("div", { className: "field-group" });
  wrapper.append(
    createElement("label", { text: "Arguments JSON" }),
    createElement("textarea", {
      text: "{}",
      attributes: { "data-raw-json": "true", "aria-label": "Arguments JSON" }
    }),
    createElement("small", { text: "The selected tool schema is not a flat object schema." })
  );
  replaceChildren(container, [wrapper]);
}

function readFieldValue(field, schema, required, name, validateRequired) {
  clearFieldError(name);
  const type = field?.dataset.fieldType || schemaType(schema);
  if (!field) {
    return undefined;
  }

  if (type === "boolean") {
    return field.checked;
  }

  const rawValue = field.value;
  if (rawValue === "" && !required) {
    return undefined;
  }
  if (rawValue === "" && required && validateRequired) {
    showFieldError(name, `${name} is required.`);
    throw new Error(`${name} is required.`);
  }
  if (rawValue === "" && required) {
    return undefined;
  }

  try {
    if (type === "integer") {
      const value = Number(rawValue);
      if (!Number.isInteger(value)) {
        throw new Error(`${name} must be an integer.`);
      }
      return value;
    }
    if (type === "number") {
      const value = Number(rawValue);
      if (!Number.isFinite(value)) {
        throw new Error(`${name} must be a number.`);
      }
      return value;
    }
    if (type === "array" || type === "object" || type === "raw") {
      return parseJsonTextarea(rawValue, name);
    }
    return rawValue;
  } catch (error) {
    showFieldError(name, error.message);
    throw error;
  }
}

function schemaType(schema) {
  if (Array.isArray(schema.type)) {
    return schema.type.find((type) => type !== "null") || "string";
  }
  return schema.type || "string";
}

function showFieldError(name, message) {
  const error = document.querySelector(`[data-field-error="${CSS.escape(name)}"]`);
  if (error) {
    error.textContent = message;
    error.classList.remove("hidden");
  }
}

function clearFieldError(name) {
  const error = document.querySelector(`[data-field-error="${CSS.escape(name)}"]`);
  if (error) {
    error.textContent = "";
    error.classList.add("hidden");
  }
}
