# Handoff: Using CodeQL to Analyze Java JAR Files for Scope Identification

## Repository
- [github/codeql](https://github.com/github/codeql)

## Objective
Leverage CodeQL to analyze Java JAR files, extract API usage patterns, and categorize code components under custom-defined scopes such as `FILE_READ`, `FILE_WRITE`, etc. (potentially 100+ custom scopes).

---

## Step-by-Step Guide

### 1. Prerequisites

- **Install Java** (JDK 8 or later)
- **Install CodeQL CLI:**  
  [Official installation guide](https://codeql.github.com/docs/codeql-cli/getting-started/)
- **(Optional) Install VS Code with the CodeQL extension** for better development experience

---

### 2. Prepare Your JAR/Project

- **If you have Java source code:** Place or clone it into a working directory.
- **If you only have a JAR:**  
  - (Recommended) Unpack classes using:  
    ```sh
    jar xf yourfile.jar
    ```
  - If needed, decompile for more analysis capability. 
  - You can also use JAR files directly for bytecode-level analysis.

---

### 3. Create a CodeQL Database

#### **If using standard source (Maven/Gradle project):**
```sh
codeql database create java-db --language=java --command="mvn compile"
```

#### **If you have only class files:**
```sh
codeql database create java-db --language=java --source-root=. --command="javac -cp yourfile.jar MainClass.java"
```
- Replace `MainClass.java` with any existing source file, or use an empty Java file if analyzing only bytecode.

---

### 4. Write CodeQL Queries for Your Scopes

- CodeQL queries are written in `.ql` files.
- Start from [Java query examples](https://github.com/github/codeql/tree/main/java/ql/src) in the repo.
- **Example:** To find file reads/writes:

```ql
import java

predicate isFileRead(Method m) {
  m.getDeclaringType().getPackage().getAName().regexpMatch("java\\.io|java\\.nio") and m.getName().matches("read%")
}
predicate isFileWrite(Method m) {
  m.getDeclaringType().getPackage().getAName().regexpMatch("java\\.io|java\\.nio") and m.getName().matches("write%")
}

from MethodAccess ma
where isFileRead(ma.getMethod())
select ma, "FILE_READ"

from MethodAccess ma
where isFileWrite(ma.getMethod())
select ma, "FILE_WRITE"
```

- **For 100+ scopes:**  
  - Define predicates for each scope based on API names/patterns you want to tag.
  - Use multiple `from ... select ...` blocks, or group outputs and label with the corresponding scope.

---

### 5. Organize Queries for Batch Execution

- Place all scope queries in a directory, e.g., `scope-queries/`.
- Create a [query suite file](https://codeql.github.com/docs/codeql-cli/query-suites/) (YAML), e.g.:

```yaml
# scope-queries.qls
- include: scope-queries/
```

---

### 6. Run Queries

```sh
codeql query run --database=java-db scope-queries.qls --output=results.bqrs
```
- To decode results:
```sh
codeql bqrs decode results.bqrs --format=csv --output=results.csv
```

---

### 7. Process Results

- Results will show which parts of the code (e.g., method names, locations) match each scope.
- Use the output for:
  - Reporting
  - Annotating code or documentation
  - Further automation (CI, dashboards)

---

### 8. Optimize and Extend

- For speed, run all queries in one suite.
- Refine predicates to minimize false positives.
- Update scope definitions as your categories evolve.

---

## Reference Links

- [CodeQL Documentation](https://codeql.github.com/docs/)
- [Java Query Examples](https://github.com/github/codeql/tree/main/java/ql/src)
- [CodeQL Query Help](https://codeql.github.com/docs/writing-codeql-queries/)

---

## Typical Workflow Example

1. **Extract JAR:** `jar xf myapp.jar`
2. **Build Database:** `codeql database create java-db --language=java --command="javac -cp myapp.jar Dummy.java"`
3. **Write Queries:** Edit or add QL files in `scope-queries/`.
4. **Organize Suite:** Edit `scope-queries.qls` as above.
5. **Run Suite:** `codeql query run ...`
6. **Get Results:** Open `results.csv` to review findings (methods/classes labeled per scope).

---

## Notes

- Adapting to non-standard build systems (not Maven/Gradle) may require custom database extraction.
- If your scopes are functionally similar, you can combine detection in a single query with multiple labels.
- Integrate into your CI pipeline for automated scope tracking.

---

## Who to Contact for Help

- A Java developer familiar with static analysis
- Anyone with experience writing CodeQL queries
- CodeQL community ([GitHub Discussions](https://github.com/github/codeql/discussions))

---

**Ready to Start? Fork the CodeQL repo, set up your workspace, and begin writing scope queries as per the requirements above.**

---

## Codex Review Notes (2026-05-29)

- CodeQL is a strong candidate for source/build-aware scope assessment. It is especially useful for custom Java queries that identify method calls, type references, and dataflow patterns that imply scopes.
- Treat CodeQL as an analyzer backend, not the source of truth for Meshingress scope policy. The repository should own a versioned scope rule catalog, then generate CodeQL query packs from that catalog.
- CodeQL Java/Kotlin database creation is build/extraction-oriented. For arbitrary uploaded JARs without source or a reproducible build, keep bytecode-oriented analysis with ASM or SootUp as the primary path.
- Suggested pipeline:

```txt
repository scope rule catalog
        -> generated CodeQL query pack for source/build-aware analysis
        -> generated ASM/SootUp matcher table for JAR-only bytecode analysis
        -> optional regex/substr matcher table for low-confidence review hints
        -> normalized findings
        -> inferredScopes
```

- Substrings and regex should remain fallback signals. They are useful for catching obvious suspicious text in source, decompiled output, resources, scripts, and config, but they should not be the only mechanism for approving or denying tool scopes.
