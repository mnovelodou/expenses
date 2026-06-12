package com.novelosoftware.expenses.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SecurityConfig.IssuerUriJwtDecoderConfig.class,
        properties = "spring.main.web-application-type=none")
class IssuerUriJwtDecoderConfigTest {

    static MockWebServer mockOidc;
    static String issuer;
    static RSAPrivateKey privateKey;

    @BeforeAll
    static void setUp() throws Exception {
        privateKey = loadPrivateKey();

        mockOidc = new MockWebServer();
        mockOidc.start();
        issuer = "http://localhost:" + mockOidc.getPort();

        // Spring fetches /.well-known/openid-configuration and then the jwks_uri on startup.
        // Queue two responses so the decoder initialises correctly.
        String jwksUri = issuer + "/jwks";
        mockOidc.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"issuer":"%s","jwks_uri":"%s"}
                        """.formatted(issuer, jwksUri)));
        mockOidc.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(jwksResponse()));
    }

    @AfterAll
    static void tearDown() throws Exception {
        mockOidc.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:" + mockOidc.getPort());
    }

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void atJwtTyp_validIssuer_isAccepted() {
        String token = buildToken(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType("at+jwt")).build(),
                issuer, validExpiry());

        assertThat(jwtDecoder.decode(token).getIssuer().toString()).isEqualTo(issuer);
    }

    @Test
    void plainJwtTyp_validIssuer_isAccepted() {
        String token = buildToken(new JWSHeader(JWSAlgorithm.RS256), issuer, validExpiry());

        assertThat(jwtDecoder.decode(token).getIssuer().toString()).isEqualTo(issuer);
    }

    @Test
    void wrongIssuer_isRejected() {
        String token = buildToken(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType("at+jwt")).build(),
                "https://evil.example.com/", validExpiry());

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("iss");
    }

    // --- helpers ---

    private static Date validExpiry() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    private static String buildToken(JWSHeader header, String tokenIssuer, Date expiry) {
        try {
            JWSSigner signer = new RSASSASigner(privateKey);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("test-user")
                    .issuer(tokenIssuer)
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

    // Minimal JWKS containing the test RSA public key
    private static String jwksResponse() throws Exception {
        byte[] bytes = IssuerUriJwtDecoderConfigTest.class
                .getResourceAsStream("/local/test-rsa-public.pem").readAllBytes();
        String pem = new String(bytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        java.security.interfaces.RSAPublicKey pub = (java.security.interfaces.RSAPublicKey)
                KeyFactory.getInstance("RSA").generatePublic(
                        new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(pem)));

        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(pub.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(pub.getPublicExponent().toByteArray());

        return """
                {"keys":[{"kty":"RSA","use":"sig","alg":"RS256","n":"%s","e":"%s"}]}
                """.formatted(n, e);
    }
}
