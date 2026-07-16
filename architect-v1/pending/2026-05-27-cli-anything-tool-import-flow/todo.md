# Todo

## Blocked Dependency

- [ ] Complete todos in `2026-05-27-meshingress-repository-artifact-implementation`.
- [ ] Confirm repository artifact upload/quarantine model exists.
- [ ] Confirm repository publication records support checksum/signature metadata.
- [ ] Confirm client/server download bridge exists for approved artifacts.

## Design Tasks

- [ ] Define generated tool artifact layout.
- [ ] Define generated tool manifest schema.
- [ ] Define publication metadata required by generated tool artifacts.
- [ ] Define trust states for generated/imported tools.
- [ ] Define review checklist for `cli-anything` generated wrappers.
- [ ] Define server install policy for approved generated tools.
- [ ] Define runtime extraction/cache behavior for embedded executables.
- [ ] Define generated wrapper argument validation rules.
- [ ] Define stdout/stderr/exit-code mapping to Meshingress result objects.
- [ ] Define minimum test suite requirements for generated tool publication.

## Implementation Tasks After Unblocked

- [ ] Create Java wrapper template for generated CLI tools.
- [ ] Create manifest parser and validator.
- [ ] Add generated-tool artifact type to repository.
- [ ] Add generated-tool validation pipeline to repository.
- [ ] Add approved publication install path to server.
- [ ] Add end-to-end prototype using a low-risk external repo.
- [ ] Add documentation for importing a repo through `cli-anything`.
