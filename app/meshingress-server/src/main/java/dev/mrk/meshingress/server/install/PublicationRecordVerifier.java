package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.artifact.model.ArtifactAssessmentSummary;
import dev.mrk.meshingress.artifact.model.ArtifactChecksum;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactProvenance;
import dev.mrk.meshingress.artifact.model.ArtifactScopeDeclaration;
import dev.mrk.meshingress.artifact.model.ArtifactTrustStatus;
import dev.mrk.meshingress.artifact.model.MeshingressArtifactType;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PublicationRecordVerifier {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String ED_25519 = "Ed25519";

    private final MeshingressProperties properties;
    private final ObjectMapper objectMapper;

    public PublicationRecordVerifier(MeshingressProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void verifySignature(ArtifactPublicationRecord publication) {
        if (publication.signature() == null || publication.signature().isBlank()) {
            throw forbidden("Publication record is unsigned.");
        }
        if (HMAC_SHA_256.equals(publication.signatureAlgorithm())) {
            verifyHmac(publication);
            return;
        }
        if (ED_25519.equals(publication.signatureAlgorithm())) {
            verifyEd25519(publication);
            return;
        }
        throw forbidden("Unsupported publication signature algorithm.");
    }

    private void verifyHmac(ArtifactPublicationRecord publication) {
        String keyId = publication.signatureKeyId() == null ? "" : publication.signatureKeyId().trim();
        String expected;
        if (keyId.isBlank()) {
            expected = sign(legacyUnsignedPayload(publication));
        } else {
            if (!properties.repository().signingKeyId().equals(keyId)) {
                throw forbidden("Unsupported publication signing key id.");
            }
            expected = sign(unsignedPayload(publication));
        }
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), publication.signature().getBytes(StandardCharsets.UTF_8))) {
            throw forbidden("Publication record signature is invalid.");
        }
    }

    private void verifyEd25519(ArtifactPublicationRecord publication) {
        String keyId = publication.signatureKeyId() == null ? "" : publication.signatureKeyId().trim();
        MeshingressProperties.Repository.PublicationVerificationKey key = properties.repository().verificationKeys().stream()
                .filter(candidate -> candidate.keyId().equals(keyId))
                .findFirst()
                .orElseThrow(() -> forbidden("Unsupported publication signing key id."));
        if (!ED_25519.equals(key.algorithm())) {
            throw forbidden("Publication signing key algorithm does not match the record.");
        }
        if (MeshingressProperties.Repository.PublicationVerificationKeyStatus.REVOKED == key.status()) {
            throw forbidden("Publication signing key is revoked.");
        }
        try {
            byte[] encodedKey = Base64.getDecoder().decode(key.publicKey());
            Signature verifier = Signature.getInstance(ED_25519);
            verifier.initVerify(KeyFactory.getInstance(ED_25519).generatePublic(new X509EncodedKeySpec(encodedKey)));
            verifier.update(unsignedPayload(publication).getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(publication.signature()))) {
                throw forbidden("Publication record signature is invalid.");
            }
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw forbidden("Publication record signature is invalid.");
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to verify publication record signature.");
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
                publication.eligibilityDecision(),
                publication.revoked(),
                publication.publishedAt(),
                publication.signatureKeyId(),
                publication.signatureAlgorithm(),
                ""
        );
        try {
            return objectMapper.writeValueAsString(unsigned);
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to serialize publication record for verification.");
        }
    }

    private String legacyUnsignedPayload(ArtifactPublicationRecord publication) {
        LegacyUnsignedPublicationRecord unsigned = new LegacyUnsignedPublicationRecord(
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

    private record LegacyUnsignedPublicationRecord(
            ArtifactCoordinate coordinate,
            MeshingressArtifactType type,
            ArtifactTrustStatus trustStatus,
            String artifactUri,
            ArtifactChecksum artifactChecksum,
            ArtifactScopeDeclaration scopePolicy,
            ArtifactAssessmentSummary scanSummary,
            ArtifactProvenance provenance,
            boolean revoked,
            java.time.OffsetDateTime publishedAt,
            String signatureAlgorithm,
            String signature
    ) {
    }
}
