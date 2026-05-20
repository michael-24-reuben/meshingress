import { InstaFetch } from "instafetch";

function readStdin() {
  return new Promise((resolve, reject) => {
    let data = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", chunk => data += chunk);
    process.stdin.on("end", () => resolve(data));
    process.stdin.on("error", reject);
  });
}

function mediaInputs(media) {
  return (media ?? []).map(item => item?.value ?? item?.shortcode ?? item);
}

function serializePath(path) {
  return {
    kind: path.kind,
    value: path.value,
    shortcode: path.shortcode,
  };
}

function serializeError(error) {
  return {
    name: error?.name ?? "Error",
    message: error?.message ?? String(error),
    status: error?.status ?? null,
  };
}

function serializeSettled(item) {
  if (item.ok) {
    return {
      path: serializePath(item.path),
      ok: true,
      data: item.data,
    };
  }

  return {
    path: serializePath(item.path),
    ok: false,
    error: serializeError(item.error),
  };
}

async function main() {
  const input = JSON.parse(await readStdin());
  const client = new InstaFetch(mediaInputs(input.media), input.options ?? {});

  switch (input.operation) {
    case "submitRequest":
      return client.submitRequest();
    case "submitAllRequests":
      return client.submitAllRequests();
    case "settleAllRequests":
      return (await client.settleAllRequests()).map(serializeSettled);
    default:
      throw new Error(`Unsupported instafetch operation: ${input.operation}`);
  }
}

try {
  const result = await main();
  console.log(JSON.stringify({ ok: true, result }));
} catch (error) {
  console.log(JSON.stringify({ ok: false, error: serializeError(error) }));
}
