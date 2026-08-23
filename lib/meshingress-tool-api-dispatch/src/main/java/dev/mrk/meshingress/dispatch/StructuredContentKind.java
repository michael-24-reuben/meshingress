package dev.mrk.meshingress.dispatch;

public interface StructuredContentKind {

    record Metadata(String kind, int version) {
        public String schema() {
            return "meshingress." + kind + ".v" + version;
        }
    }

    Metadata contentData();

    default String value() {
        return contentData().kind();
    }

    default int version() {
        return contentData().version();
    }

    default String schema() {
        return contentData().schema();
    }

    enum Text implements StructuredContentKind {
        TEXT_PLAIN("text.plain", 1),
        TEXT_MARKDOWN("text.markdown", 1),
        TEXT_HTML("text.html", 1),
        TEXT_DOCUMENT("text.document", 1),
        TEXT_CODE("text.code", 1),
        TEXT_LOG("text.log", 1);

        private final Metadata contentData;

        Text(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    /**
     * Canonical envelope kinds for parsed JSON whose field-level schema is not declared by the
     * producing tool. The subtype identifies only the root JSON shape; it is not a claim that
     * the payload fields are stable.
     */
    enum GeneratedJson implements StructuredContentKind {
        OBJECT("generated.json.object", 1),
        ARRAY("generated.json.array", 1),
        STRING("generated.json.string", 1),
        NUMBER("generated.json.number", 1),
        BOOLEAN("generated.json.boolean", 1),
        NULL("generated.json.null", 1);

        private final Metadata contentData;

        GeneratedJson(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Media implements StructuredContentKind {
        MEDIA_VIDEO("media.video", 1),
        MEDIA_IMAGE("media.image", 1),
        MEDIA_AUDIO("media.audio", 1),
        MEDIA_GALLERY("media.gallery", 1),
        MEDIA_PLAYLIST("media.playlist", 1),
        MEDIA_CAPTION("media.caption", 1),
        MEDIA_TRANSCRIPT("media.transcript", 1);

        private final Metadata contentData;

        Media(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum File implements StructuredContentKind {
        FILE_GENERIC("file.generic", 1),
        FILE_ARCHIVE("file.archive", 1),
        FILE_DIRECTORY("file.directory", 1),
        FILE_UPLOAD("file.upload", 1),
        FILE_DOWNLOAD("file.download", 1),
        FILE_MANIFEST("file.manifest", 1),
        STORAGE_LOCATION("storage.location", 1);

        private final Metadata contentData;

        File(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Web implements StructuredContentKind {
        WEB_PAGE("web.page", 1),
        WEB_HTML("web.html", 1),
        WEB_LINK("web.link", 1),
        WEB_LINKS("web.links", 1),
        WEB_METADATA("web.metadata", 1),
        WEB_SCREENSHOT("web.screenshot", 1),
        WEB_DOM("web.dom", 1),
        WEB_ELEMENT("web.element", 1);

        private final Metadata contentData;

        Web(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Process implements StructuredContentKind {
        PROCESS_EXECUTION("process.execution", 1),
        PROCESS_STDOUT("process.stdout", 1),
        PROCESS_STDERR("process.stderr", 1),
        PROCESS_LOG("process.log", 1),
        PROCESS_DIAGNOSTIC("process.diagnostic", 1),
        PROCESS_STATUS("process.status", 1);

        private final Metadata contentData;

        Process(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Data implements StructuredContentKind {
        DATA_RECORD("data.record", 1),
        DATA_RECORDS("data.records", 1),
        DATA_TABLE("data.table", 1),
        DATA_TREE("data.tree", 1),
        DATA_GRAPH("data.graph", 1),
        DATA_TIMELINE("data.timeline", 1),
        DATA_METRICS("data.metrics", 1);

        private final Metadata contentData;

        Data(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Search implements StructuredContentKind {
        SEARCH_RESULT("search.result", 1),
        SEARCH_RESULTS("search.results", 1),
        RETRIEVAL_CONTEXT("retrieval.context", 1),
        RETRIEVAL_MEMORY("retrieval.memory", 1),
        RETRIEVAL_CITATIONS("retrieval.citations", 1);

        private final Metadata contentData;

        Search(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Message implements StructuredContentKind {
        MESSAGE_EMAIL("message.email", 1),
        MESSAGE_CHAT("message.chat", 1),
        MESSAGE_NOTIFICATION("message.notification", 1),
        MESSAGE_THREAD("message.thread", 1),
        MESSAGE_DELIVERY("message.delivery", 1);

        private final Metadata contentData;

        Message(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Identity implements StructuredContentKind {
        IDENTITY_USER("identity.user", 1),
        IDENTITY_ACCOUNT("identity.account", 1),
        IDENTITY_PRINCIPAL("identity.principal", 1),
        IDENTITY_SESSION("identity.session", 1),
        IDENTITY_AUTH("identity.auth", 1);

        private final Metadata contentData;

        Identity(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Tool implements StructuredContentKind {
        TOOL_DESCRIPTOR("tool.descriptor", 1),
        TOOL_LIST("tool.list", 1),
        TOOL_CALL("tool.call", 1),
        TOOL_RESULT("tool.result", 1),
        RUNTIME_STATUS("runtime.status", 1),
        RUNTIME_HEALTH("runtime.health", 1),
        RUNTIME_CONFIG("runtime.config", 1);

        private final Metadata contentData;

        Tool(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Security implements StructuredContentKind {
        SECURITY_SCOPE("security.scope", 1),
        SECURITY_POLICY("security.policy", 1),
        SECURITY_DECISION("security.decision", 1),
        SECURITY_APPROVAL("security.approval", 1),
        AUDIT_RECORD("audit.record", 1),
        AUDIT_RECORDS("audit.records", 1);

        private final Metadata contentData;

        Security(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Time implements StructuredContentKind {
        TIME_INSTANT("time.instant", 1),
        TIME_RANGE("time.range", 1),
        CALENDAR_EVENT("calendar.event", 1),
        CALENDAR_EVENTS("calendar.events", 1),
        SCHEDULE_JOB("schedule.job", 1),
        SCHEDULE_TRIGGER("schedule.trigger", 1);

        private final Metadata contentData;

        Time(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Location implements StructuredContentKind {
        LOCATION_POINT("location.point", 1),
        LOCATION_PLACE("location.place", 1),
        LOCATION_ROUTE("location.route", 1),
        LOCATION_MAP("location.map", 1),
        LOCATION_REGION("location.region", 1);

        private final Metadata contentData;

        Location(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Network implements StructuredContentKind {
        NETWORK_HTTP_REQUEST("network.http.request", 1),
        NETWORK_HTTP_RESPONSE("network.http.response", 1),
        NETWORK_WEBSOCKET_EVENT("network.websocket.event", 1),
        API_RESPONSE("api.response", 1),
        API_ENDPOINT("api.endpoint", 1),
        API_SCHEMA("api.schema", 1);

        private final Metadata contentData;

        Network(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }

    enum Diagnostic implements StructuredContentKind {
        DIAGNOSTIC_RESULT("diagnostic.result", 1),
        DIAGNOSTIC_ISSUE("diagnostic.issue", 1),
        DIAGNOSTIC_REPORT("diagnostic.report", 1),
        REPORT_SUMMARY("report.summary", 1),
        REPORT_FINDINGS("report.findings", 1),
        REPORT_ASSESSMENT("report.assessment", 1);

        private final Metadata contentData;

        Diagnostic(String value, int version) {
            this.contentData = new Metadata(value, version);
        }

        @Override
        public Metadata contentData() {
            return contentData;
        }
    }
}
