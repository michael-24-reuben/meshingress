# Context

`docs/meshingress-openapi_v2.yaml` was produced by a separate export path and flattened the MCP request contract. The current Springdoc `/v3/api-docs` test verifies the MCP annotations remain intact.

The artifact and storage routes did not have comparable source-level operation descriptions. This slice adds them directly to the controller contract and verifies the live Springdoc document. It must not change authorization, artifact lifecycle, storage retrieval, or response behavior.
