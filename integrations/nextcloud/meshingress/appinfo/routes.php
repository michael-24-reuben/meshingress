<?php

declare(strict_types=1);

return [
    'ocs' => [
        ['name' => 'source_import#create', 'url' => '/api/v1/source-imports', 'verb' => 'POST'],
        ['name' => 'source_import#show', 'url' => '/api/v1/source-imports/{jobId}', 'verb' => 'GET'],
        ['name' => 'source_import#initialize', 'url' => '/api/v1/workspace/initialize', 'verb' => 'POST'],
        ['name' => 'delegated_workspace#reserve', 'url' => '/api/v1/delegated-workspaces', 'verb' => 'POST'],
        ['name' => 'delegated_workspace#upload', 'url' => '/api/v1/delegated-workspaces/{workspaceId}/files', 'verb' => 'PUT'],
        ['name' => 'delegated_workspace#append_sources', 'url' => '/api/v1/delegated-workspaces/{workspaceId}/sources', 'verb' => 'POST'],
        ['name' => 'delegated_workspace#seal', 'url' => '/api/v1/delegated-workspaces/{workspaceId}/seal', 'verb' => 'POST'],
        ['name' => 'delegated_workspace#show', 'url' => '/api/v1/delegated-workspaces/{workspaceId}', 'verb' => 'GET'],
        ['name' => 'delegated_workspace#show_by_request', 'url' => '/api/v1/delegated-workspaces/by-request/{requestId}', 'verb' => 'GET'],
    ],
];
