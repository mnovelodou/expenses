package com.novelosoftware.expenses;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

public class TestJwtFactory {

    private static final RSAPrivateKey PRIVATE_KEY;

    static {
        try {
            byte[] bytes = TestJwtFactory.class.getResourceAsStream("/local/test-rsa-private.pem").readAllBytes();
            String pem = new String(bytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PRIVATE_KEY = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test RSA private key", e);
        }
    }

    public static String createToken(String... scopes) {
        return buildToken(new Date(System.currentTimeMillis() + 3_600_000), scopes);
    }

    public static String createExpiredToken(String... scopes) {
        return buildToken(new Date(System.currentTimeMillis() - 3_600_000), scopes);
    }

    private static String buildToken(Date expiry, String... scopes) {
        try {
            JWSSigner signer = new RSASSASigner(PRIVATE_KEY);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("test-user")
                    .issuer("test-issuer")
                    .expirationTime(expiry)
                    .claim("scp", String.join(" ", scopes))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test JWT", e);
        }
    }
}
