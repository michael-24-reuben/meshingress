/**
 * Meshingress Nextcloud delegated-external client.
 *
 * Runtime requirements:
 * - Node.js 18+ or a browser/runtime with global fetch
 * - A Nextcloud username/user ID and app password
 *
 * The current delegated-workspace workflow is:
 *   1. reserveDelegatedWorkspace()
 *   2. uploadReservedFile() for native tool output
 *   3. appendDelegatedSources() for HTTPS media
 *   4. sealDelegatedWorkspace()
 *   5. waitForDelegatedWorkspace(requestId)
 *
 * The prepareWorkspace()/delegateDownload() methods remain only for legacy
 * /source-imports compatibility and must not be used for new integrations.
 */

const DELEGATED_REQUEST_TYPE = 'meshingress.delegated-download/v1';
const DELEGATED_JOB_TYPE = 'meshingress.delegated-download-job/v1';
const STORAGE_MANIFEST_TYPE = 'meshingress.tool-storage-manifest/v1';

export class MeshingressApiError extends Error {
    constructor(message, {status = 0, url = '', body = null} = {}) {
        super(message);
        this.name = 'MeshingressApiError';
        this.status = status;
        this.url = url;
        this.body = body;
    }
}

export class NextcloudDelegatedStorageClient {
    #baseUrl;
    #username;
    #userId;
    #authorization;
    #fetch;

    constructor({baseUrl, username, appPassword, userId = username, authorization, fetchImpl = globalThis.fetch,}) {
        if (typeof fetchImpl !== 'function') {
            throw new TypeError(
                'fetchImpl must be provided when global fetch is unavailable.',
            );
        }

        this.#baseUrl = normalizeBaseUrl(baseUrl);
        this.#fetch = fetchImpl;

        if (authorization !== undefined && authorization !== null) {
            if (username !== undefined || appPassword !== undefined) {
                throw new TypeError(
                    'Provide either authorization or username/appPassword, not both.',
                );
            }

            const basic = NextcloudDelegatedStorageClient.parseBasicAuthorization(authorization);

            this.#username = basic.userId;
            this.#userId = requireText(userId ?? basic.userId, 'userId');
            this.#authorization = basic.authorization;
            return;
        }

        this.#username = requireText(username, 'username');
        this.#userId = requireText(userId, 'userId');
        this.#authorization =
            `Basic ${encodeBasicAuth(
                this.#username,
                requireText(appPassword, 'appPassword'),
            )}`;
    }

    /**
     * Parses a Basic Authorization header.
     * @param {string} authorization - The authorization header.
     * @returns {{authorization: string, userId: string}} The parsed authorization.
     */
    static parseBasicAuthorization(authorization) {
        const value = requireText(authorization, 'authorization');
        const match = /^Basic\s+([A-Za-z0-9+/]+={0,2})$/i.exec(value);

        if (!match) {
            throw new TypeError(
                'authorization must use the format "Basic <base64(userId:password)>".',
            );
        }

        let decoded;

        try {
            if (typeof Buffer !== 'undefined') {
                decoded = Buffer.from(match[1], 'base64').toString('utf8');
            } else {
                decoded = decodeURIComponent(
                    Array.from(atob(match[1]), character =>
                        `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`,
                    ).join(''),
                );
            }
        } catch {
            throw new TypeError('authorization contains invalid Base64 data.');
        }

        const separator = decoded.indexOf(':');

        if (separator <= 0) {
            throw new TypeError(
                'authorization must encode a non-empty "userId:password" value.',
            );
        }

        const userId = decoded.slice(0, separator);
        const password = decoded.slice(separator + 1);

        if (!password) {
            throw new TypeError('authorization must encode a non-empty password.');
        }

        return {
            authorization: `Basic ${match[1]}`,
            userId,
        };
    }

    /** Initializes Workspace/Meshingress and its managed base directories. */
    async initializeWorkspace() {
        return this.#ocsRequest('/ocs/v2.php/apps/meshingress/api/v1/workspace/initialize?format=json', {
            method: 'POST',
            body: {},
        });
    }

    /** Reserves an OPEN Nextcloud workspace; no source import starts here. */
    async reserveDelegatedWorkspace({toolId, sessionId, requestId}) {
        const response = await this.#ocsRequest(
            '/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces?format=json',
            {
                method: 'POST',
                headers: {'X-Meshingress-Tool-Id': validateToolId(toolId)},
                body: {
                    sessionId: validateIdentifier(sessionId, 'sessionId'),
                    requestId: validateIdentifier(requestId, 'requestId'),
                },
            },
        );
        if (response?.type !== 'meshingress.delegated-workspace/v1' || response?.state !== 'OPEN') {
            throw new MeshingressApiError('The server did not return an OPEN delegated workspace.', {body: response});
        }
        return response;
    }

    /** Uploads tool-generated native content while the reservation is OPEN. */
    async uploadReservedFile(workspace, toolId, relativePath, data, {contentType = 'application/octet-stream'} = {}) {
        const workspaceId = requireText(workspace?.workspaceId, 'workspace.workspaceId');
        const path = validateRelativeFilePath(relativePath, 'relativePath');
        return this.#ocsRequest(
            `/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/${encodeURIComponent(workspaceId)}/files?format=json`,
            {
                method: 'PUT',
                headers: {'X-Meshingress-Tool-Id': validateToolId(toolId)},
                body: {path, contentBase64: base64Content(data), contentType},
            },
        );
    }

    /** Atomically appends delegated HTTPS sources without starting the worker. */
    async appendDelegatedSources(workspace, toolId, sources, {baseUrl = undefined} = {}) {
        const workspaceId = requireText(workspace?.workspaceId, 'workspace.workspaceId');
        if (!Array.isArray(sources) || sources.length === 0) throw new TypeError('sources must be a non-empty array.');
        const normalizedBase = baseUrl === undefined ? undefined : new URL(baseUrl);
        const normalized = sources.map((source, index) => {
            const rawUrl = requireText(source?.url, `sources[${index}].url`);
            const url = new URL(rawUrl, normalizedBase).href;
            if (new URL(url).protocol !== 'https:') throw new TypeError(`sources[${index}].url must resolve to HTTPS.`);
            const path = source?.path ?? source?.outlet?.path ?? new URL(url).pathname.replace(/^\/+/, '');
            return {url, path: validateRelativeFilePath(path, `sources[${index}].path`)};
        });
        return this.#ocsRequest(
            `/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/${encodeURIComponent(workspaceId)}/sources?format=json`,
            {method: 'POST', headers: {'X-Meshingress-Tool-Id': validateToolId(toolId)}, body: {sources: normalized}},
        );
    }

    /** Seals a workspace once and queues its single Nextcloud import job. */
    async sealDelegatedWorkspace(workspace, toolId) {
        const workspaceId = requireText(workspace?.workspaceId, 'workspace.workspaceId');
        return this.#ocsRequest(
            `/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/${encodeURIComponent(workspaceId)}/seal?format=json`,
            {method: 'POST', headers: {'X-Meshingress-Tool-Id': validateToolId(toolId)}, body: {}},
        );
    }

    async getDelegatedWorkspace(requestId) {
        return this.#ocsRequest(
            `/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces/by-request/${encodeURIComponent(validateIdentifier(requestId, 'requestId'))}?format=json`,
            {method: 'GET'},
        );
    }

    async waitForDelegatedWorkspace(requestId, options = {}) {
        const {pollIntervalMs = 1_000, timeoutMs = 15 * 60_000, onUpdate = undefined} = options;
        const startedAt = Date.now();
        while (true) {
            const status = await this.getDelegatedWorkspace(requestId);
            if (typeof onUpdate === 'function') await onUpdate(status);
            if (status?.type === STORAGE_MANIFEST_TYPE && status?.state === 'COMPLETED') return status;
            if (status?.state === 'FAILED') throw new MeshingressApiError(status.error || 'The delegated workspace failed.', {body: status});
            if (Date.now() - startedAt >= timeoutMs) throw new MeshingressApiError(`Timed out waiting for delegated workspace ${requestId}.`, {body: status});
            await sleep(pollIntervalMs);
        }
    }

    /** @deprecated Use reserveDelegatedWorkspace() and uploadReservedFile(). */
    async prepareWorkspace({toolId, rootPath}) {
        const normalizedToolId = validateToolId(toolId);
        const normalizedRootPath = validateRelativeDirectoryPath(rootPath, 'rootPath');

        await this.initializeWorkspace();

        const workspacePath = [
            'Workspace',
            'Meshingress',
            'storage',
            normalizedToolId,
            ...normalizedRootPath.split('/'),
        ].join('/');

        const workspaceUri = this.#davUri(workspacePath, true);
        await this.#ensureDavDirectory(workspaceUri);

        return {
            toolId: normalizedToolId,
            rootPath: normalizedRootPath,
            workspacePath,
            workspaceUri,
        };
    }

    /** @deprecated Use appendDelegatedSources() and sealDelegatedWorkspace(). */
    async delegateDownload({
       toolId,
       sources,
       baseUrl = undefined,
       rootPath = undefined,
       sessionId = undefined,
       requestId = undefined,
    }) {
        const payload = createDelegatedDownloadPayload({
            sources,
            baseUrl,
            rootPath,
            sessionId,
            requestId,
        });

        const response = await this.#ocsRequest(
            '/ocs/v2.php/apps/meshingress/api/v1/source-imports?format=json',
            {
                method: 'POST',
                headers: {
                    'X-Meshingress-Tool-Id': validateToolId(toolId),
                },
                body: payload,
            },
        );

        if (response?.type !== DELEGATED_JOB_TYPE) {
            throw new MeshingressApiError('The server returned an unexpected delegated-download response.', {
                body: response,
            });
        }

        return response;
    }

    /** Reads queued, running, failed, or completed delegated-import state. */
    async getJob(jobId) {
        const normalizedJobId = validateJobId(jobId);
        return this.#ocsRequest(
            `/ocs/v2.php/apps/meshingress/api/v1/source-imports/${encodeURIComponent(normalizedJobId)}?format=json`,
            {method: 'GET'},
        );
    }

    /** Polls until the server returns the completed storage manifest or a failed state. */
    async waitForCompletion(jobId, {
        pollIntervalMs = 1_000,
        timeoutMs = 15 * 60_000,
        signal = undefined,
        onUpdate = undefined,
    } = {}) {
        const normalizedJobId = validateJobId(jobId);
        const startedAt = Date.now();

        while (true) {
            if (signal?.aborted) {
                throw signal.reason ?? new DOMException('The operation was aborted.', 'AbortError');
            }

            const status = await this.getJob(normalizedJobId);
            if (typeof onUpdate === 'function') {
                await onUpdate(status);
            }

            if (status?.type === STORAGE_MANIFEST_TYPE && status?.state === 'COMPLETED') {
                return status;
            }

            if (status?.state === 'FAILED') {
                throw new MeshingressApiError(status.error || 'The delegated download failed.', {
                    body: status,
                });
            }

            if (Date.now() - startedAt >= timeoutMs) {
                throw new MeshingressApiError(`Timed out waiting for delegated job ${normalizedJobId}.`, {
                    body: status,
                });
            }

            await sleep(pollIntervalMs, signal);
        }
    }

    /**
     * Uploads a Meshingress-native file such as book.json into a prepared or returned workspace.
     * Parent directories are created automatically through WebDAV MKCOL.
     */
    async uploadNativeFile(workspaceUri, relativePath, data, {
        contentType = 'application/octet-stream',
        overwrite = false,
    } = {}) {
        const path = validateRelativeFilePath(relativePath, 'relativePath');
        const rootUri = ensureDirectoryUrl(workspaceUri);
        const parentSegments = path.split('/').slice(0, -1);

        if (parentSegments.length > 0) {
            let current = rootUri;
            for (const segment of parentSegments) {
                current = new URL(`${encodeURIComponent(segment)}/`, current).href;
                await this.#mkcol(current);
            }
        }

        const fileUri = new URL(encodePath(path), rootUri).href;
        const headers = {
            Authorization: this.#authorization,
            'Content-Type': contentType,
        };
        if (!overwrite) {
            headers['If-None-Match'] = '*';
        }

        const response = await this.#fetch(fileUri, {
            method: 'PUT',
            headers,
            body: normalizeUploadBody(data),
        });

        if (!response.ok) {
            throw await this.#httpError(response, fileUri, `Could not upload native file ${path}.`);
        }

        return {
            path,
            uri: fileUri,
            status: response.status,
            etag: response.headers.get('etag'),
        };
    }

    async uploadNativeJson(workspaceUri, relativePath, value, {
        pretty = true,
        overwrite = false,
    } = {}) {
        const json = JSON.stringify(value, null, pretty ? 2 : 0) + (pretty ? '\n' : '');
        return this.uploadNativeFile(workspaceUri, relativePath, json, {
            contentType: 'application/json; charset=utf-8',
            overwrite,
        });
    }

    /** @deprecated Legacy WebDAV-predelegation convenience flow. */
    async delegatePreparedWorkspace({
                                        toolId,
                                        rootPath,
                                        nativeFiles = [],
                                        sources,
                                        baseUrl = undefined,
                                        sessionId = undefined,
                                        requestId = undefined,
                                        wait = true,
                                        waitOptions = undefined,
                                    }) {
        const workspace = await this.prepareWorkspace({toolId, rootPath});

        for (const file of nativeFiles) {
            if (!file || typeof file !== 'object') {
                throw new TypeError('Each nativeFiles entry must be an object.');
            }
            await this.uploadNativeFile(workspace.workspaceUri, file.path, file.data, {
                contentType: file.contentType ?? inferContentType(file.path),
                overwrite: file.overwrite ?? false,
            });
        }

        const job = await this.delegateDownload({
            toolId,
            rootPath: workspace.rootPath,
            sources,
            baseUrl,
            sessionId,
            requestId,
        });

        if (!wait) {
            return {workspace, job};
        }

        const result = await this.waitForCompletion(job.jobId, waitOptions);
        return {workspace, job, result};
    }

    #davUri(path, directory = false) {
        const encodedPath = encodePath(path);
        const suffix = directory ? '/' : '';
        return new URL(
            `/remote.php/dav/files/${encodeURIComponent(this.#userId)}/${encodedPath}${suffix}`,
            this.#baseUrl,
        ).href;
    }

    async #ensureDavDirectory(directoryUri) {
        const target = new URL(directoryUri);
        const davPrefix = `/remote.php/dav/files/${encodeURIComponent(this.#userId)}/`;
        if (!target.pathname.startsWith(davPrefix)) {
            throw new TypeError('workspaceUri is outside the configured Nextcloud user WebDAV root.');
        }

        const relative = decodeURIComponent(target.pathname.slice(davPrefix.length));
        const segments = relative.split('/').filter(Boolean);
        let current = new URL(davPrefix, this.#baseUrl).href;

        for (const segment of segments) {
            current = new URL(`${encodeURIComponent(segment)}/`, current).href;
            await this.#mkcol(current);
        }
    }

    async #mkcol(uri) {
        const response = await this.#fetch(uri, {
            method: 'MKCOL',
            headers: {Authorization: this.#authorization},
        });

        // 201 = created; 405 = already exists.
        if (response.status !== 201 && response.status !== 405) {
            throw await this.#httpError(response, uri, 'Could not create the WebDAV directory.');
        }
    }

    async #ocsRequest(path, {method, headers = {}, body = undefined, rawBody = undefined, contentType = undefined}) {
        const url = new URL(path, this.#baseUrl).href;
        const requestHeaders = {
            Authorization: this.#authorization,
            'OCS-APIRequest': 'true',
            Accept: 'application/json',
            ...headers,
        };

        let requestBody;
        if (rawBody !== undefined) {
            requestHeaders['Content-Type'] = contentType ?? 'application/octet-stream';
            requestBody = rawBody;
        } else if (body !== undefined) {
            requestHeaders['Content-Type'] = 'application/json';
            requestBody = JSON.stringify(body);
        }

        const response = await this.#fetch(url, {
            method,
            headers: requestHeaders,
            body: requestBody,
        });

        const text = await response.text();
        let parsed = null;
        if (text !== '') {
            try {
                parsed = JSON.parse(text);
            } catch {
                throw new MeshingressApiError('The server returned non-JSON data.', {
                    status: response.status,
                    url,
                    body: text,
                });
            }
        }

        const ocsStatusCode = Number(parsed?.ocs?.meta?.statuscode ?? 0);
        const data = parsed?.ocs?.data ?? parsed;

        if (!response.ok || ocsStatusCode >= 400) {
            const message =
                data?.message ||
                parsed?.ocs?.meta?.message ||
                `Nextcloud request failed with HTTP ${response.status}.`;
            throw new MeshingressApiError(message, {
                status: response.status,
                url,
                body: parsed,
            });
        }

        return data;
    }

    async #httpError(response, url, fallbackMessage) {
        const text = await response.text().catch(() => '');
        return new MeshingressApiError(text || fallbackMessage, {
            status: response.status,
            url,
            body: text,
        });
    }
}

/** Builds the exact JSON request accepted by the Nextcloud Meshingress app. */
export function createDelegatedDownloadPayload({
                                                   sources,
                                                   baseUrl = undefined,
                                                   rootPath = undefined,
                                                   sessionId = undefined,
                                                   requestId = undefined,
                                               }) {
    if (!Array.isArray(sources) || sources.length === 0) {
        throw new TypeError('sources must be a non-empty array.');
    }

    const payload = {
        type: DELEGATED_REQUEST_TYPE,
        sources: sources.map((source, index) => normalizeSource(source, index)),
    };

    if (baseUrl !== undefined && baseUrl !== null && String(baseUrl).trim() !== '') {
        const normalizedBaseUrl = new URL(String(baseUrl));
        if (normalizedBaseUrl.protocol !== 'https:') {
            throw new TypeError('baseUrl must use HTTPS.');
        }
        normalizedBaseUrl.hash = '';
        if (normalizedBaseUrl.search !== '') {
            throw new TypeError('baseUrl must not contain a query string.');
        }
        if (!normalizedBaseUrl.pathname.endsWith('/')) {
            normalizedBaseUrl.pathname += '/';
        }
        payload.baseUrl = normalizedBaseUrl.href;
    }

    if (rootPath !== undefined && rootPath !== null && String(rootPath).trim() !== '') {
        payload.rootPath = validateRelativeDirectoryPath(rootPath, 'rootPath');
    }
    if (sessionId !== undefined && sessionId !== null && String(sessionId).trim() !== '') {
        payload.sessionId = validateIdentifier(sessionId, 'sessionId');
    }
    if (requestId !== undefined && requestId !== null && String(requestId).trim() !== '') {
        payload.requestId = validateIdentifier(requestId, 'requestId');
    }

    const usesRelativeSource = payload.sources.some(({url}) => !hasUrlScheme(url));
    if (usesRelativeSource && !payload.baseUrl) {
        throw new TypeError('baseUrl is required when any source URL is relative.');
    }

    return payload;
}

function normalizeSource(source, index) {
    if (!source || typeof source !== 'object' || Array.isArray(source)) {
        throw new TypeError(`sources[${index}] must be an object.`);
    }

    const url = requireText(source.url, `sources[${index}].url`);
    if (url.startsWith('//')) {
        throw new TypeError(`sources[${index}].url must not be protocol-relative.`);
    }
    if (hasUrlScheme(url) && new URL(url).protocol !== 'https:') {
        throw new TypeError(`sources[${index}].url must use HTTPS.`);
    }

    const normalized = {url};
    const outletPath = source.outlet?.path;
    if (outletPath !== undefined && outletPath !== null && String(outletPath).trim() !== '') {
        normalized.outlet = {
            path: validateRelativeFilePath(outletPath, `sources[${index}].outlet.path`),
        };
    }
    return normalized;
}

function validateToolId(value) {
    const toolId = requireText(value, 'toolId');
    if (!/^[a-z0-9][a-z0-9._-]{0,127}$/.test(toolId)) {
        throw new TypeError('toolId must be a stable lowercase tool identifier.');
    }
    return toolId;
}

function validateJobId(value) {
    const jobId = requireText(value, 'jobId');
    if (!/^nci_[a-f0-9]{32}$/.test(jobId)) {
        throw new TypeError('jobId is invalid.');
    }
    return jobId;
}

function validateIdentifier(value, field) {
    const text = requireText(value, field);
    if (text.length > 128 || /[\x00-\x1F\x7F]/.test(text)) {
        throw new TypeError(`${field} is invalid.`);
    }
    return text;
}

function validateRelativeDirectoryPath(value, field) {
    const path = normalizeRelativePath(value);
    if (path === '') {
        throw new TypeError(`${field} must be a non-empty relative directory path.`);
    }
    validatePathSegments(path, field);
    return path;
}

function validateRelativeFilePath(value, field) {
    const path = normalizeRelativePath(value);
    if (path === '' || path.endsWith('/')) {
        throw new TypeError(`${field} must identify a relative file path.`);
    }
    validatePathSegments(path, field);
    return path;
}

function validatePathSegments(path, field) {
    for (const segment of path.split('/')) {
        if (segment === '' || segment === '.' || segment === '..' || segment.includes('\0')) {
            throw new TypeError(`${field} contains an invalid path segment.`);
        }
    }
}

function normalizeRelativePath(value) {
    return requireText(value, 'path')
        .replaceAll('\\', '/')
        .replace(/^\/+|\/+$/g, '');
}

function normalizeBaseUrl(value) {
    const url = new URL(requireText(value, 'baseUrl'));
    if (url.protocol !== 'https:' && url.hostname !== 'localhost' && url.hostname !== '127.0.0.1') {
        throw new TypeError('Nextcloud baseUrl must use HTTPS except for localhost development.');
    }
    url.pathname = url.pathname.replace(/\/+$/, '') + '/';
    url.search = '';
    url.hash = '';
    return url.href;
}

function requireText(value, field) {
    if (typeof value !== 'string' || value.trim() === '') {
        throw new TypeError(`${field} is required.`);
    }
    return value.trim();
}

function hasUrlScheme(value) {
    return /^[A-Za-z][A-Za-z0-9+.-]*:/.test(value);
}

function encodePath(path) {
    return path.split('/').map(encodeURIComponent).join('/');
}

function ensureDirectoryUrl(value) {
    const url = new URL(requireText(value, 'workspaceUri'));
    if (!url.pathname.endsWith('/')) {
        url.pathname += '/';
    }
    return url.href;
}

function normalizeUploadBody(data) {
    if (
        typeof data === 'string' ||
        data instanceof ArrayBuffer ||
        ArrayBuffer.isView(data) ||
        (typeof Blob !== 'undefined' && data instanceof Blob)
    ) {
        return data;
    }
    throw new TypeError('Upload data must be a string, Blob, ArrayBuffer, Buffer, or typed array.');
}

function base64Content(data) {
    if (typeof data === 'string') {
        if (typeof Buffer !== 'undefined') return Buffer.from(data, 'utf8').toString('base64');
        return btoa(unescape(encodeURIComponent(data)));
    }
    if (typeof Buffer !== 'undefined' && (Buffer.isBuffer(data) || ArrayBuffer.isView(data) || data instanceof ArrayBuffer)) {
        return Buffer.from(data).toString('base64');
    }
    throw new TypeError('Reserved native content must be a string, ArrayBuffer, Buffer, or typed array.');
}

function inferContentType(path) {
    const extension = path.toLowerCase().split('.').pop();
    return {
        json: 'application/json; charset=utf-8',
        jsonl: 'application/x-ndjson; charset=utf-8',
        txt: 'text/plain; charset=utf-8',
        md: 'text/markdown; charset=utf-8',
        html: 'text/html; charset=utf-8',
        jpg: 'image/jpeg',
        jpeg: 'image/jpeg',
        png: 'image/png',
        webp: 'image/webp',
        pdf: 'application/pdf',
        zip: 'application/zip',
    }[extension] ?? 'application/octet-stream';
}

function encodeBasicAuth(username, password) {
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    if (typeof Buffer !== 'undefined') {
        return Buffer.from(bytes).toString('base64');
    }
    let binary = '';
    for (const byte of bytes) binary += String.fromCharCode(byte);
    return btoa(binary);
}

function sleep(milliseconds, signal) {
    return new Promise((resolve, reject) => {
        const timeout = setTimeout(resolve, milliseconds);
        if (!signal) return;

        const abort = () => {
            clearTimeout(timeout);
            reject(signal.reason ?? new DOMException('The operation was aborted.', 'AbortError'));
        };
        if (signal.aborted) abort();
        else signal.addEventListener('abort', abort, {once: true});
    });
}

export {
    DELEGATED_REQUEST_TYPE,
    DELEGATED_JOB_TYPE,
    STORAGE_MANIFEST_TYPE,
};

/*
Example:

import { NextcloudDelegatedStorageClient } from './nextcloud-delegated-storage-client.mjs';

const client = new NextcloudDelegatedStorageClient({
  baseUrl: 'https://nextcloud.example.com',
  username: process.env.NEXTCLOUD_USERNAME,
  appPassword: process.env.NEXTCLOUD_APP_PASSWORD,
});

const { workspace, job, result } = await client.delegatePreparedWorkspace({
  toolId: 'toonverse.download-book',
  rootPath: 'books/solo-leveling',
  sessionId: '2c1f521f-cbdf-9a26-0b6f-e7df436f8132',
  requestId: 'req_72fe36e3a1a948b58c4eba3a33baaf11',
  baseUrl: 'https://source.example.com/solo-leveling/',
  nativeFiles: [
    {
      path: 'book.json',
      data: JSON.stringify({ title: 'Solo Leveling' }, null, 2) + '\n',
      contentType: 'application/json; charset=utf-8',
    },
  ],
  sources: [
    { url: 'cover.jpg' },
    { url: 'chapters/001/page1.jpg' },
    {
      url: 'assets/page2-a81f920.jpg?token=temporary',
      outlet: { path: 'chapters/001/page2.jpg' },
    },
  ],
  waitOptions: {
    pollIntervalMs: 1_000,
    onUpdate: status => console.log(status.state),
  },
});

console.log({ workspace, job, result });
*/
