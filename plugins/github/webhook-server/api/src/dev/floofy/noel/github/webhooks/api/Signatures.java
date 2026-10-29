/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2026 Noel Towa <cutie@floofy.dev>
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package dev.floofy.noel.github.webhooks.api;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/// Webhook IDs, secrets, and GitHub's signature check.
///
/// GitHub signs each delivery with the webhook's secret and sends the result
/// as `X-Hub-Signature-256: sha256=<hex>`, an HMAC-SHA256 of the raw body. A
/// delivery is only trusted if that signature matches, which proves it came from
/// someone who knows the secret; the webhook's ID in the URL alone proves
/// nothing, since it's visible in GitHub's settings to every repository admin.
public final class Signatures {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "sha256=";

    private Signatures() {}

    /// Returns a new webhook ID: 20 URL-safe characters (120 random bits), so IDs
    /// can't be guessed or enumerated.
    public static String newId() {
        final var bytes = new byte[15];
        RANDOM.nextBytes(bytes);

        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    /// Returns a new webhook secret: 64 hex characters (256 random bits).
    public static String newSecret() {
        final var bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    /// Computes the `X-Hub-Signature-256` value GitHub sends for a body.
    /// @param secret the webhook's secret
    /// @param body   the raw request body
    /// @return `sha256=` followed by the lowercase hex HMAC
    public static String sign(String secret, byte[] body) {
        Objects.requireNonNull(secret, "secret");
        Objects.requireNonNull(body, "body");

        try {
            final var mac = Mac.getInstance("HmacSHA256");
            final var keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            return String.format("%s%s", PREFIX, HexFormat.of().formatHex(mac.doFinal(body)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("failed to sign", ex);
        }
    }

    /// Checks a delivery's signature.
    ///
    /// The comparison takes the same time however much of the signature
    /// matches, so an attacker can't work it out byte by byte from response
    /// times.
    ///
    /// @param secret the webhook's secret
    /// @param body   the raw request body, exactly as received
    /// @param header the `X-Hub-Signature-256` header, or `null` if missing
    /// @return whether the signature is present and correct
    public static boolean verify(String secret, byte[] body, String header) {
        if (header == null || !header.startsWith(PREFIX)) {
            return false;
        }

        final var expected = sign(secret, body).getBytes(StandardCharsets.US_ASCII);
        final var given = header.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, given);
    }
}
