package com.novelosoftware.expenses.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuerUriJwtDecoderConfigTest {

    private static final String ISSUER = "https://dev-pvjriuvkhfjjreft.us.auth0.com/";
    private static RSAPrivateKey privateKey;
    private static JwtDecoder decoder;

    @BeforeAll
    static void setUp() throws Exception {
        privateKey = loadPrivateKey();
        RSAPublicKey publicKey = loadPublicKey();

        // Mirror the exact customizations applied in IssuerUriJwtDecoderConfig
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .jwtProcessorCustomizer(p -> p.setJWSTypeVerifier(
                        new DefaultJOSEObjectTypeVerifier<>(
                                new JOSEObjectType("at+jwt"),
                                JOSEObjectType.JWT,
                                null)))
                .build();
        nimbusDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        decoder = nimbusDecoder;
    }

    @Test
    void atJwtTyp_validIssuer_isAccepted() {
        String token = buildToken(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("at+jwt"))
                .build(), ISSUER, validExpiry());

        assertThat(decoder.decode(token).getIssuer().toString()).isEqualTo(ISSUER);
    }

    @Test
    void plainJwtTyp_validIssuer_isAccepted() {
        String token = buildToken(new JWSHeader(JWSAlgorithm.RS256), ISSUER, validExpiry());

        assertThat(decoder.decode(token).getIssuer().toString()).isEqualTo(ISSUER);
    }

    @Test
    void wrongIssuer_isRejected() {
        String token = buildToken(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("at+jwt"))
                .build(), "https://evil.example.com/", validExpiry());

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("iss");
    }

    // --- helpers ---

    private static Date validExpiry() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    private static String buildToken(JWSHeader header, String issuer, Date expiry) {
        try {
            JWSSigner signer = new RSASSASigner(privateKey);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("test-user")
                    .issuer(issuer)
                    .expirationTime(expiry)
                    .claim("scope", "read:accounts")
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static RSAPrivateKey loadPrivateKey() throws Exception {
        byte[] bytes = IssuerUriJwtDecoderConfigTest.class
                .getResourceAsStream("/local/test-rsa-private.pem").readAllBytes();
        String pem = new String(bytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }

    private static RSAPublicKey loadPublicKey() throws Exception {
        byte[] bytes = IssuerUriJwtDecoderConfigTest.class
                .getResourceAsStream("/local/test-rsa-public.pem").readAllBytes();
        String pem = new String(bytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }
}
