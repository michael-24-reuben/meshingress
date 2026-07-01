package dev.mrk.meshingress.artifact.publication;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

public class Ed25519PublicationRecordSigner implements PublicationRecordSigner {

    public static final String ALGORITHM = "Ed25519";

    private final String keyId;
    private final PrivateKey privateKey;

    public Ed25519PublicationRecordSigner(String keyId, PrivateKey privateKey) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Ed25519 publication signing key id is required");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Ed25519 publication private key is required");
        }
        this.keyId = keyId.trim();
        this.privateKey = privateKey;
    }

    @Override
    public String keyId() {
        return keyId;
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }

    @Override
    public PublicationSignature sign(String payload) {
        try {
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(privateKey);
            signer.update(payload.getBytes(StandardCharsets.UTF_8));
            return new PublicationSignature(keyId, ALGORITHM, Base64.getEncoder().encodeToString(signer.sign()));
        } catch (Exception exception) {
            throw new PublicationSigningException("unable to sign publication record", exception);
        }
    }
}
