# Brief: Meshingress Repository Artifact Implementation

## Goal

Create `app/meshingress-repository`, a Maven-like artifact repository for Meshingress-managed artifacts. It should be understandable in the same way `mvnrepository.com/artifact` is understandable: a central place to upload, inspect, assess, approve, and publish versioned artifacts.

The repository must support more than installable tools. Initial scope includes generated CLI tool modules, ordinary tool modules, availability annotation artifacts, policy bundles, schemas, SBOMs, signatures, attestations, and future runtime/plugin artifacts.

## Core Problem

Generated wrappers from tools such as `cli-anything` can make external repositories easy to expose as Meshingress tools, but they create a supply-chain and runtime-security risk. The official MCP runtime should not accept arbitrary generated JARs or executables directly.

The system needs a bridge-stage repository that:

- accepts uploaded artifacts;
- quarantines new uploads;
- extracts and normalizes artifact metadata;
- computes checksums;
- scans for vulnerabilities, malware, suspicious patterns, secrets, and policy violations;
- produces review records;
- records requested scopes vs approved scopes;
- signs approved publication records;
- exposes approved artifacts for official MCP runtime installation.

## Decision

Use the final structure:

```txt
meshingress/
├─ app/
│  ├─ meshingress-server/
│  └─ meshingress-repository/
│
├─ lib/
│  ├─ meshingress-tool-api/
│  ├─ meshingress-tool-annotations/
│  ├─ meshingress-tool-framework/
│  ├─ meshingress-artifact-model/
│  ├─ meshingress-artifact-storage/
│  ├─ meshingress-artifact-security/
│  └─ meshingress-artifact-publication/
│
├─ toolspace/
│  ├─ first-party/
│  └─ generated/
│
└─ repository/
   ├─ artifacts/
   ├─ metadata/
   ├─ indexes/
   ├─ quarantine/
   ├─ assessments/
   ├─ reviews/
   ├─ sbom/
   ├─ attestations/
   └─ signatures/
```

## Scope Boundaries

### In Scope

- Artifact coordinate model based on Maven-like `groupId:artifactId:version`.
- Artifact type model for Meshingress-specific assets.
- Upload and quarantine flow.
- Security assessment pipeline.
- Review and trust-status state machine.
- Publication record generation.
- Checksum and signature verification flow for the MCP runtime.
- Local filesystem storage backend for development.
- Storage interface that can later support S3, OCI registry, or a Maven-compatible backend.

### Out of Scope for First Implementation

- Full marketplace UI.
- Public multi-tenant hosting.
- Complete dynamic runtime plugin loading.
- Fully automated trust without a review gate.
- Building a custom malware engine.
- Building a custom vulnerability database.

## Non-Negotiable Rule

The official MCP runtime must only install artifacts from signed publication records that are in an installable trust state and whose checksums match exactly.
