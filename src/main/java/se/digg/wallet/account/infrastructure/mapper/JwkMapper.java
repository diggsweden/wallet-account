// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.infrastructure.model.StoredJwk;

public class JwkMapper {

  public static StoredJwk toStoredJwk(PublicKeyDto key) {
    Objects.requireNonNull(key, "key");
    Map<String, Object> members = new LinkedHashMap<>();
    putIfPresent(members, "kty", key.kty());
    putIfPresent(members, "kid", key.kid());
    putIfPresent(members, "alg", key.alg());
    putIfPresent(members, "use", key.use());
    putIfPresent(members, "crv", key.crv());
    putIfPresent(members, "x", key.x());
    putIfPresent(members, "y", key.y());
    try {
      JWK jwk = JWK.parse(members).toPublicJWK();
      return new StoredJwk(jwk.toJSONString(), jwk.getKeyID(),
          jwk.computeThumbprint().toString());
    } catch (ParseException | JOSEException ex) {
      throw new IllegalArgumentException("Invalid JWK: " + ex.getMessage(), ex);
    }
  }

  public static PublicKeyDto toPublicKeyDto(StoredJwk storedJwk) {
    Objects.requireNonNull(storedJwk, "storedJwk");
    Objects.requireNonNull(storedJwk.getJwk(), "storedJwk.jwk");
    try {
      Map<String, Object> members = JWK.parse(storedJwk.getJwk()).toJSONObject();
      return new PublicKeyDto(
          (String) members.get("kty"),
          (String) members.get("kid"),
          (String) members.get("alg"),
          (String) members.get("use"),
          (String) members.get("crv"),
          (String) members.get("x"),
          (String) members.get("y"));
    } catch (ParseException ex) {
      throw new IllegalStateException("Stored JWK is not parseable: " + ex.getMessage(), ex);
    }
  }

  private static void putIfPresent(Map<String, Object> members, String name, String value) {
    if (value != null) {
      members.put(name, value);
    }
  }

  private JwkMapper() {
    // no instantiation
  }
}
