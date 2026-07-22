import {
  NextcloudDelegatedStorageClient,
} from './nextcloud-delegated-storage-client.mjs';

if (!process.env.NEXTCLOUD_PRIMARY_AUTH) {
  throw new Error('NEXTCLOUD_PRIMARY_AUTH must contain Basic <base64(userId:appPassword)>.');
}

const client = new NextcloudDelegatedStorageClient({
  baseUrl: 'https://NEXTCLOUD_SERVER_URL',
  // Env value expects `Basic <USERID:APP_PASSWORD>` base64 encoded string, e.g. `Basic YWxwaGFzdW5ueTpHNDNwRi1rdzdLai1iVHo5ZC1Xc3RpYi1OU20ycw==`
  authorization: process.env.NEXTCLOUD_PRIMARY_AUTH,
});
// Alternatively, you can use username and app password for authentication:
/*const client = new NextcloudDelegatedStorageClient({
  baseUrl: 'https://NEXTCLOUD_SERVER_URL',
  username: "<USERID>",
  appPassword: "<APP_PASSWORD>",
});*/

const toolId = 'toonverse.download-book';
const requestId = `req_delegated_test_${Date.now()}`;
const sessionId = `delegated-test-${Date.now()}`;
const workspace = await client.reserveDelegatedWorkspace({toolId, sessionId, requestId});
console.log('Reserved OPEN workspace:', workspace.workspaceUri);

await client.uploadReservedFile(workspace, toolId, 'book.json', JSON.stringify({
  type: 'open-ink.book/v1',
  source: 'pinterest-test',
  title: 'Delegated workspace smoke test',
}, null, 2) + '\n', {contentType: 'application/json'});

await client.appendDelegatedSources(workspace, toolId, [
  {url: '736x/80/c8/9f/80c89f27c0ee6086bb962d8f83ceb61e.jpg'},
  {url: '1200x/82/11/94/82119497c13dbf414d5e4f20c0a61e79.jpg'},
  {url: '736x/05/93/b6/0593b61e58f0271a86697c0ccd59ef26.jpg', path: 'images/knight-and-cat.jpg'},
], {baseUrl: 'https://i.pinimg.com/'});

const queued = await client.sealDelegatedWorkspace(workspace, toolId);
console.log('Sealed workspace:', queued.state, {jobId: queued.jobId});

const result = await client.waitForDelegatedWorkspace(requestId, {
  pollIntervalMs: 1_000,
  onUpdate(status) {
    console.log('Delegated download:', status.state, {
      attempts: status.attempts,
      error: status.error ?? null,
    });
  },
});

console.log('Completed response:', result);
