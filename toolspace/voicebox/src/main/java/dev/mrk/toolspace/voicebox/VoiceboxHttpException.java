package dev.mrk.toolspace.voicebox;

import tools.jackson.databind.node.ObjectNode;

final class VoiceboxHttpException extends RuntimeException {

    private final ObjectNode response;

    VoiceboxHttpException(String message, ObjectNode response) {
        super(message);
        this.response = response;
    }

    ObjectNode response() {
        return response;
    }
}
