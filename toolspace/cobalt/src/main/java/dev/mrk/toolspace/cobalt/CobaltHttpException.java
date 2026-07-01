package dev.mrk.toolspace.cobalt;

import tools.jackson.databind.node.ObjectNode;

final class CobaltHttpException extends RuntimeException {

    private final ObjectNode response;

    CobaltHttpException(String message, ObjectNode response) {
        super(message);
        this.response = response;
    }

    ObjectNode response() {
        return response;
    }
}
