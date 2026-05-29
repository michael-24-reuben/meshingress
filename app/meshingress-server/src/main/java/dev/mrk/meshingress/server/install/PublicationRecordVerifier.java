package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class PublicationRecordVerifier {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final MeshingressProperties properties;
    private final ObjectMapper objectMapper;

    public PublicationRecordVerifier(MeshingressProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void verifySignature(ArtifactPublicationRecord publication) {
        if (!HMAC_SHA_256.equals(publication.signatureAlgorithm())) {
            throw forbidden("Unsupported publication signature algorithm.");
        }
        if (publication.signature() == null || publication.signature().isBlank()) {
            throw forbidden("Publication record is unsigned.");
        }
        String expected = sign(unsignedPayload(publication));
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), publication.signature().getBytes(StandardCharsets.UTF_8))) {
            throw forbidden("Publication record signature is invalid.");
        }
    }

    private String unsignedPayload(ArtifactPublicationRecord publication) {
        ArtifactPublicationRecord unsigned = new ArtifactPublicationRecord(
                publication.coordinate(),
                publication.type(),
                publication.trustStatus(),
                publication.artifactUri(),
                publication.artifactChecksum(),
                publication.scopePolicy(),
                publication.scanSummary(),
                publication.provenance(),
                publication.revoked(),
                publication.publishedAt(),
                "",
                ""
        );
        try {
            return objectMapper.writeValueAsString(unsigned);
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to serialize publication record for verification.");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(properties.repository().signingSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to verify publication record signature.");
        }
    }

    private JsonRpcException forbidden(String message) {
        return new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, message);
    }
}
