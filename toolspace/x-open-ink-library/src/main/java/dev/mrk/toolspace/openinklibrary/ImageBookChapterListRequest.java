package dev.mrk.toolspace.openinklibrary;

public record ImageBookChapterListRequest(
        String sourceWorkId,
        Integer limit,
        String order,
        String authorizationToken
) {
}
