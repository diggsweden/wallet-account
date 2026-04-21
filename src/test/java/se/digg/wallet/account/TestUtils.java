// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.KeyRevocation;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import java.net.URI;
import java.security.KeyStore;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

public class TestUtils {

  public static PublicKeyEntity generateJwkEntity(String kid) {
    return new PublicKeyEntity(
        "EC",
        kid,
        "alg",
        "enc",
        "P-256",
        "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
        "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM");
  }

  public static PublicKeyDtoBuilder publicKeyDtoBuilderWithDefaults(String kid) {
    return PublicKeyDtoBuilder.builder()
        .kty("EC")
        .crv("P-256")
        .x("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4")
        .y("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM")
        .alg("alg")
        .use("enc")
        .kid(kid);
  }

  public static Map<String, Object> createJwk() {
    final KeyType kty = KeyType.EC;
    final KeyUse use = null; // KeyUse.ENCRYPTION;
    final Set<KeyOperation> ops = new HashSet<>();
    final Algorithm alg = null; // Algorithm.NONE;
    final String kid = null; // "kid";
    final URI x5u = null; // new URI("http://example.com");
    final Base64URL x5t = null; // new Base64URL("url");
    final Base64URL x5t256 = null; // new Base64URL("url");
    final List<Base64> x5c = null; // List.of(new Base64("BASE64"));
    final Date exp = null; // new Date();
    final Date nbf = null; // new Date();
    final Date iat = null; // new Date();
    // final Date revokedAt = new Date();
    final KeyRevocation revocation = null; // new KeyRevocation(revokedAt,
                                           // KeyRevocation.Reason.UNSPECIFIED);
    final KeyStore ks = null; // KeyStore.getInstance(KeyStore.getDefaultType());

    Map<String, Object> result = new HashMap<>();
    result.put("kty", kty);
    result.put("use", use);
    result.put("ops", ops);
    result.put("alg", alg);
    result.put("kid", kid);
    result.put("x5u", x5u);
    result.put("x5t", x5t);
    result.put("x5t256", x5t256);
    result.put("x5c", x5c);
    result.put("exp", exp);
    result.put("nbf", nbf);
    result.put("iat", iat);
    result.put("revocation", revocation);
    result.put("ks", ks);

    return result;
  }

  public static AccountDtoBuilder accountDtoBuilderWithDefaults() {
    return AccountDtoBuilder.builder()
        .emailAdress("dummy@dummy.se")
        .personalIdentityNumber("720202-0234")
        .publicKey(publicKeyDtoBuilderWithDefaults("99").build());
  }
}
