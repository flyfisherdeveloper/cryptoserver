package com.scanner.cryptoserver.exchange.coinbasepro;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.management.RuntimeErrorException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class Signature {
    private static Mac sharedMac;

    /**
     * Generate a by creating a sha256 HMAC using
     * the base64-decoded secret key on the prehash string for:
     * timestamp + method + requestPath + body (where + represents string concatenation)
     * and base64-encode the output.
     * The timestamp value is the same as the CB-ACCESS-TIMESTAMP header.
     *
     * @param requestPath
     * @param method
     * @param body
     * @param timestamp
     * @return
     */
    public String generate(String requestPath, String method, String body, String timestamp) {
        try {
            String secretKey = System.getenv("SecretKey");
            if (sharedMac == null) {
                sharedMac = Mac.getInstance("HmacSHA256");
            }
            String prehash = timestamp + method.toUpperCase() + requestPath + body;
            byte[] secretDecoded = Base64.getDecoder().decode(secretKey);
            SecretKeySpec keyspec = new SecretKeySpec(secretDecoded, sharedMac.getAlgorithm());
            Mac sha256 = (Mac) sharedMac.clone();
            sha256.init(keyspec);
            return Base64.getEncoder().encodeToString(sha256.doFinal(prehash.getBytes()));
        } catch (CloneNotSupportedException | InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeErrorException(new Error("Cannot set up authentication headers."));
        }
    }
}
