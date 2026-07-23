<?php

declare(strict_types=1);

return [
    'ocs' => [
        ['name' => 'workspace#reserve', 'url' => '/api/v1/delegated-workspaces', 'verb' => 'POST'],
        ['name' => 'workspace#upload', 'url' => '/api/v1/delegated-workspaces/{workspaceId}/files', 'verb' => 'PUT'],
        ['name' => 'workspace#append_sources', 'url' => '/api/v1/delegated-workspaces/{workspaceId}/sources', 'verb' => 'POST'],
        ['name' => 'workspace#seal', 'url' => '/api/v1/delegated-workspaces/{workspaceId}/seal', 'verb' => 'POST'],
        ['name' => 'workspace#show', 'url' => '/api/v1/delegated-workspaces/{workspaceId}', 'verb' => 'GET'],
        ['name' => 'workspace#show_by_request', 'url' => '/api/v1/delegated-workspaces/by-request/{requestId}', 'verb' => 'GET'],
    ],
];
