## Dispatch method injection

`McpProgressReporter` is an infrastructure parameter, not a client-supplied
JSON argument. A tool method can append it without `@McpDispatchParam`:

```java
DispatchExecutionResult downloadBook(
        ToonverseDownloadBookArgs arguments,
        McpCallContext context,
        McpProgressReporter progress
) {
    // Reporting adoption is optional for now.
}
```

The current annotated tool and route invocation layers create a fresh no-op reporter for every invocation.
It does not serialize, upload, or send progress events. Delivery wiring remains
an explicit later transport decision.

## Intended use case

A tool handler receives or creates one reporter **per tool invocation**, then reports lifecycle events:

```java
public DispatchExecutionResult downloadBook(
        List<String> chapterUrls,
        McpProgressReporter progress
) {
    int total = chapterUrls.size();

    progress.plan(
            Duration.ofMinutes(5),
            total,
            List.of("downloading", "validating", "packaging"),
            "Preparing to download " + total + " chapters"
    );

    try {
        for (int index = 0; index < total; index++) {
            int completed = index + 1;

            progress.update(
                    "downloading",
                    completed,
                    total,
                    "Downloading chapter " + completed
            );

            downloadChapter(chapterUrls.get(index));

            /*
             * Retains:
             * phase = "downloading"
             * unitsCompleted = completed
             * total = total
             */
            progress.update("Checking downloaded chapter");
        }

        /*
         * The latest coordinates are retained here as well.
         */
        progress.update(
                "validating",
                total,
                total,
                "Validating downloaded chapters"
        );

        validateChapters();

        progress.update(
                "packaging",
                total,
                total,
                "Creating book archive"
        );

        Path archive = packageBook();

        progress.complete("Book archive created: " + archive);

        return DispatchExecutionResult.builder()
                .text("Book downloaded successfully")
                .build();

    } catch (Exception exception) {
        progress.error("Book download failed: " + exception.getMessage());

        return DispatchExecutionResult.builder()
                .error("BOOK_DOWNLOAD_FAILED", exception.getMessage())
                .build();
    }
}
```

## Retained progress behavior

These calls:

```java
progress.update("downloading", 4, 10, "Downloaded chapter 4");
progress.update("Verifying chapter metadata");
progress.warning("Chapter title was missing");
progress.complete("Download completed");
```

Produce events equivalent to:

```text
IN_PROGRESS
phase=downloading
completed=4
total=10
message=Downloaded chapter 4

IN_PROGRESS
phase=downloading
completed=4
total=10
message=Verifying chapter metadata

WARNING
phase=downloading
completed=4
total=10
message=Chapter title was missing

COMPLETED_SUCCESSFUL
phase=downloading
completed=4
total=10
message=Download completed
```

Only the detailed overload changes the stored progress coordinates:

```java
progress.update(
        "processing",
        5,
        10,
        "Processing chapter 5"
);
```

The simple overload changes only the message:

```java
progress.update("Applying metadata");
```

## Console usage

For local development or testing:

```java
McpProgressReporter progress = McpProgressReporter.console();

progress.plan(
        Duration.ofSeconds(30),
        3,
        List.of("download", "process"),
        "Starting"
);

progress.update("download", 1, 3, "Downloaded first file");
progress.update("Checking file integrity");
progress.update("download", 2, 3, "Downloaded second file");
progress.complete("Finished");
```

Example output:

```text
[MCP PLAN] estimate=PT30S total=3 phases=[download, process] message=Starting
[MCP UPDATE] phase=download 1/3 Downloaded first file
[MCP UPDATE] phase=download 1/3 Checking file integrity
[MCP UPDATE] phase=download 2/3 Downloaded second file
[MCP COMPLETE] phase=download 2/3 Finished
```

## WebSocket usage

The WebSocket layer supplies the event sender:

```java
McpProgressReporter progress = McpProgressReporter.webSocket(
        progressUpdate -> {
            ObjectNode event = objectMapper.createObjectNode()
                    .put("state", progressUpdate.state().name())
                    .put("phase", progressUpdate.phase())
                    .put("unitsCompleted", progressUpdate.unitsCompleted())
                    .put("total", progressUpdate.total())
                    .put("message", progressUpdate.message());

            webSocketSession.sendMessage(
                    new TextMessage(event.toString())
            );
        }
);
```

The tool handler does not need to know whether progress is going to the console, WebSocket, logs, or nowhere:

```java
progress.update("downloading", 3, 10, "Downloaded chapter 3");
```

## Important correction to the draft

`Reporter.CONSOLE` and `Reporter.NOOP` should not be mutable singleton reporter instances. `Reporter` retains progress and terminal state, so sharing one instance causes separate tool calls to share state. After the first operation calls `complete()`, later operations can fail with the terminal-state exception.

Create a fresh internal reporter for each invocation:

```java
public static McpProgressReporter console() {
    return new McpProgressReporter(new ConsoleReporter());
}

public static McpProgressReporter webSocket(
        Consumer<ProgressUpdate> sender
) {
    return new McpProgressReporter(
            new MeshigressWebSocketReporter(sender)
    );
}
```

For no-op reporting, either provide a fresh instance:

```java
public static McpProgressReporter noop() {
    return new McpProgressReporter(new NoopReporter());
}
```

or make the no-op implementation genuinely stateless and exempt from terminal-state tracking.

The core usage rule is:

```java
McpProgressReporter reporter = McpProgressReporter.console();
```

Create one reporter per tool execution, use detailed updates when progress coordinates change, and use message-only updates when only the status text changes.
