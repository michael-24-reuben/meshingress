# Plan

1. Inspect classpath and runtime module registration seams and define one runtime module-catalog entry that keeps the manifest and safe local resource access together.
2. Add public module list, detail, README, and icon endpoints. Keep metadata, properties, requirements, and license inline; return README as `text/markdown` and icon as its declared image media type.
3. Register both Spring/classpath modules and installed artifact modules through the same catalog API without relying on a JAR-only repository coordinate.
4. Add the module reference and icon URL to the Studio's live tool mapping, retaining the folder fallback when a module has no icon.
5. Test bounded public responses, absent icon/README handling, source-module resources, and Studio production compilation.
