# Fixes

## Files Changed

- `app/meshingress-repository/pom.xml`
- `app/meshingress-repository/src/main/resources/application.properties`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryProperties.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryConfiguration.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/ArtifactMetadataStore.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/ArtifactMetadataEntry.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/SqlArtifactMetadataStore.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java`

## Behavioral Changes

- `ArtifactService` delegates canonical metadata persistence to `ArtifactMetadataStore`.
- SQL is the only active metadata-store implementation in this slice.
- `record.json`, `latest-review.json`, and `publication.json` are no longer written as canonical metadata.
- Repository SQL table identity is property-driven through `meshingress.repository.sql.*`.
- The focused repository flow proves a fresh SQL store instance can reload artifact, assessment, and publication metadata after the API flow completes.
