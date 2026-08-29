/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.security.keys;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 *
 * @author danil
 */
public final class PemKeys {

    private PemKeys() {
    }

    public static RSAPublicKey parsePublicKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("Public key PEM is blank");
        }
        if (pem.contains("BEGIN RSA PUBLIC KEY")) {
            throw new IllegalArgumentException(
                    "Unsupported PEM format: BEGIN RSA PUBLIC KEY. Expected BEGIN PUBLIC KEY (X.509)."
            );
        }

        try {
            String normalized = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] der = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);

        } catch (IllegalArgumentException e) {
            // Base64 decode обычно кидает IllegalArgumentException
            throw new IllegalStateException("Public key PEM is not valid Base64 / X.509", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to parse RSA public key", e);
        }
    }
}
