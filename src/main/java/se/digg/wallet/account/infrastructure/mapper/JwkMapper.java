// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.infrastructure.model.StoredJwk;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Maps between the public key DTO and a stored JWK. The DTO's fields are named after the JWK
 * members, so the conversion goes through a plain json object.
 */
public class JwkMapper {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

  public static StoredJwk toStoredJwk(PublicKeyDto key) {
    Objects.requireNonNull(key, "key");
    try {
      JWK jwk = JWK.parse(MAPPER.convertValue(key, JSON_OBJECT)).toPublicJWK();
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
      return MAPPER.convertValue(JWK.parse(storedJwk.getJwk()).toJSONObject(), PublicKeyDto.class);
    } catch (ParseException ex) {
      throw new IllegalStateException("Stored JWK is not parseable: " + ex.getMessage(), ex);
    }
  }

  private JwkMapper() {
    // no instantiation
  }
}
