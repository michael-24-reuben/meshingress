package dev.mrk.meshingress.artifact.publication;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class HmacPublicationRecordSigner implements PublicationRecordSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public HmacPublicationRecordSigner(String secret) {
        String value = secret == null || secret.isBlank() ? "dev-repository-signing-key" : secret;
        this.secret = value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public PublicationSignature sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return new PublicationSignature(ALGORITHM, HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception exception) {
            throw new PublicationSigningException("unable to sign publication record", exception);
        }
    }
}
