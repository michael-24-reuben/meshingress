package dev.mrk.toolspace.instagram.instafetch;

public enum FetchPathKind {
    URL("url"),
    SHORTCODE("shortcode");

    private final String jsonValue;

    FetchPathKind(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public String jsonValue() {
        return jsonValue;
    }
}
