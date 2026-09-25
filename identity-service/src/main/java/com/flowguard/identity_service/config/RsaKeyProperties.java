package com.flowguard.identity_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record RsaKeyProperties(
        /*
         * The RSAPublicKey and RSAPrivateKey are used for signing and verifying JWT tokens.
         * The public key is used to verify the signature of the token,
         * while the private key is used to sign the token.
         * The keys are loaded from the application properties file using the @ConfigurationProperties annotation.
         */
        RSAPublicKey publicKey,
        RSAPrivateKey privateKey,
        String issuer,
        Duration accessTokenExpiration
) {
}

