#!/usr/bin/env node
/*
 * Backend-only dispatcher extracted from ritz078/transform.  The browser UI,
 * Next pages, components, assets, styles and Web Worker wrapper are deliberately
 * absent; this process reads one JSON request from stdin and writes one JSON result.
 */
const fs = require('fs');

const fail = error => { process.stdout.write(JSON.stringify({ ok: false, error: error.message || String(error) })); };
const json = value => JSON.parse(value);
const pretty = value => JSON.stringify(value, null, 2);

async function transform({ type, value, secondaryValue = '', settings = {} }) {
  switch (type) {
    case 'json-to-yaml': return require('yaml').stringify(json(value));
    case 'yaml-to-json': return JSON.stringify(require('yaml').parse(value));
    case 'yaml-to-toml': return require('@iarna/toml').stringify(require('yaml').parse(value));
    case 'toml-to-json': return JSON.stringify(require('@iarna/toml').parse(value));
    case 'toml-to-yaml': return require('yaml').stringify(require('@iarna/toml').parse(value));
    case 'xml-to-json': return JSON.stringify(JSON.parse(require('xml-js').xml2json(value, { compact: true })));
    case 'markdown-to-html': return require('markdown').markdown.toHTML(value);
    case 'html-to-pug': return require('html2pug')(value, settings);
    case 'html-to-jsx': {
      const HtmlToJsx = require('htmltojsx');
      return new HtmlToJsx({ createClass: false }).convert(value);
    }
    case 'json-to-graphql': return require('@walmartlabs/json-to-simple-graphql-schema').jsonToSchema(json(value));
    case 'json-to-zod': return require('json-to-zod').jsonToZod(json(value));
    case 'json-schema-to-zod': return require('json-schema-to-zod').jsonSchemaToZod(json(value));
    case 'json-schema-to-typescript': return require('json-schema-to-typescript').compile(json(value), settings.name || 'Root');
    case 'json-schema-to-openapi-schema': return pretty(await require('@openapi-contrib/json-schema-to-openapi-schema')(json(value), { cloneSchema: true }));
    case 'jsonld-to-expanded': return pretty(await require('jsonld').expand(json(value)));
    case 'jsonld-to-compacted': return pretty(await require('jsonld').compact(json(value), json(secondaryValue)));
    case 'jsonld-to-flattened': return pretty(await require('jsonld').flatten(json(value), json(secondaryValue)));
    case 'jsonld-to-framed': return pretty(await require('jsonld').frame(json(value), json(secondaryValue)));
    case 'jsonld-to-normalized': return require('jsonld').normalize(json(value));
    case 'jsonld-to-nquads': return require('jsonld').toRDF(json(value), { format: 'application/n-quads' });
    case 'css-to-js': {
      const postcss = require('postcss'); const postcssJs = require('postcss-js');
      return `const converted = ${JSON.stringify(postcssJs.objectify(postcss.parse(value)))}`;
    }
    case 'svg-to-jsx': return require('@svgr/core').default(value, { plugins: [require('@svgr/plugin-jsx')], svgo: false });
    case 'svg-to-react-native': return require('@svgr/core').default(value, { plugins: [require('@svgr/plugin-jsx')], svgo: false, native: true });
    default: return transformCodegen({ type, value, secondaryValue, settings });
  }
}

async function transformCodegen({ type, value, secondaryValue, settings }) {
  const aliases = {
    'json-to-typescript': 'typescript', 'json-to-java': 'java', 'json-to-kotlin': 'kotlin',
    'json-to-rust-serde': 'rust', 'json-to-json-schema': 'json-schema',
    'json-to-scala-case-class': 'scala', 'json-to-go': 'go', 'json-to-go-bson': 'go'
  };
  if (aliases[type]) {
    const wasm = await import('./node_modules/json_typegen_wasm/json_typegen_wasm.js');
    const outputMode = aliases[type] === 'json-schema' ? 'json_schema' : aliases[type];
    const kotlin = type === 'json-to-java';
    const generated = wasm.run('Root', value, JSON.stringify({ ...(settings || {}), output_mode: kotlin ? 'kotlin' : outputMode }));
    return kotlin ? kotlinToJava(generated) : generated;
  }
  const graphqlTypes = {
    'graphql-to-typescript': ['@graphql-codegen/typescript', '@graphql-codegen/typescript-operations'],
    'graphql-to-flow': ['@graphql-codegen/flow', '@graphql-codegen/flow-operations'],
    'graphql-to-java': ['@graphql-codegen/java'],
    'graphql-to-introspection-json': ['@graphql-codegen/introspection'],
    'graphql-to-fragment-matcher': ['@graphql-codegen/fragment-matcher'],
    'graphql-to-schema-ast': ['@graphql-codegen/schema-ast'],
    'graphql-to-typescript-mongodb': ['@graphql-codegen/typescript', '@graphql-codegen/typescript-operations', '@graphql-codegen/typescript-mongodb']
  };
  if (graphqlTypes[type]) {
    const { codegen } = require('@graphql-codegen/core');
    const { parse } = require('graphql');
    const plugins = graphqlTypes[type].map(name => require(name));
    const pluginMap = {}; plugins.forEach((plugin, index) => pluginMap[index + 1] = plugin);
    return codegen({ filename: `a.${type.includes('json') ? 'json' : 'ts'}`, schema: parse(value),
      plugins: plugins.map((_plugin, index) => ({ [index + 1]: {} })),
      documents: secondaryValue.trim() ? [{ location: '', document: parse(secondaryValue) }] : [], config: {}, pluginMap });
  }
  if (type === 'typescript-to-javascript') return require('@babel/core').transformSync(value, { plugins: [require('@babel/plugin-transform-typescript')], configFile: false }).code;
  if (type === 'flow-to-javascript') return require('@babel/core').transformSync(value, { plugins: [require('@babel/plugin-transform-flow-strip-types')], configFile: false }).code;
  if (type === 'flow-to-typescript') return require('@khanacademy/flow-to-ts').convert(value);
  if (type === 'typescript-to-flow') { const flowgen = require('flowgen'); return flowgen.beautify(flowgen.compiler.compileDefinitionString(value)); }
  if (type === 'json-to-mongoose') return require('generate-schema').mongoose(json(value));
  if (type === 'json-to-mysql') return require('generate-schema').mysql(json(value));
  if (type === 'json-to-big-query') return require('generate-schema').bigquery(json(value));
  if (type === 'json-to-proptypes') return require('@babel/standalone').transform(`const propTypes = ${value}`, { plugins: [require('babel-plugin-json-to-proptypes')] }).code;
  if (type === 'js-object-to-json') return pretty(Function(`"use strict"; return (${value});`)());
  if (type === 'js-object-to-typescript') return (await import('./node_modules/json_typegen_wasm/json_typegen_wasm.js')).run('Root', JSON.stringify(Function(`"use strict"; return (${value});`)()), JSON.stringify({ ...(settings || {}), output_mode: 'typescript' }));
  throw new Error(`Unsupported Transform type in the backend extraction: ${type}`);
}

// Preserves Transform's json-to-java strategy: generate Kotlin, then expand data classes.
function kotlinToJava(source) {
  let output = '', current = '', names = [], types = [];
  for (const original of source.split('\n')) {
    const line = original.trim();
    if (line === ')') {
      const args = names.map((name, i) => `${types[i]} ${name}`).join(', ');
      output += `\n\tpublic ${current}(${args}) {\n${names.map(name => `\t\tthis.${name} = ${name};`).join('\n')}\n\t}\n`;
      for (let i = 0; i < names.length; i++) { const cap = names[i][0].toUpperCase() + names[i].slice(1); output += `\n\tpublic ${types[i]} get${cap}() {\n\t\treturn this.${names[i]};\n\t}\n\n\tpublic void set${cap}(${types[i]} ${names[i]}) {\n\t\tthis.${names[i]} = ${names[i]};\n\t}\n`; }
      output += '}\n'; current = ''; names = []; types = [];
    } else if (line.startsWith('data class ')) {
      current = line.slice(11, line.indexOf('(')); output += `public class ${current} {\n`;
    } else if (line.startsWith('val ')) {
      const field = line.replace('?', ''); const colon = field.indexOf(':'); const name = field.slice(4, colon); const type = field.slice(colon + 2).replace(/,$/, '').replace('<Any>', '<?>');
      names.push(name); types.push(type); output += `\tprivate ${type} ${name};\n`;
    } else if (line.startsWith('import ')) output += `${line};\n`;
    else output += `${original}\n`;
  }
  return output;
}

(async () => {
  try {
    const input = JSON.parse(fs.readFileSync(0, 'utf8'));
    const result = await transform(input);
    process.stdout.write(JSON.stringify({ ok: true, result: String(result) }));
  } catch (error) { fail(error); }
})();
