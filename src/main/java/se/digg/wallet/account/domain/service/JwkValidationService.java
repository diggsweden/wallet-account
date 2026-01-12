// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.digg.wallet.account.application.model.PublicKeyDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwkValidationService {
  Logger log = LoggerFactory.getLogger(JwkValidationService.class);

  private final ObjectMapper objectMapper;

  public JwkValidationService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public boolean validateJwk(PublicKeyDto jwkDto) {
    String jwkJson;
    try {
      jwkJson = objectMapper.writeValueAsString(jwkDto);
    } catch (JacksonException e) {
      log.error("Unable to create json from internal object", e);
      return false;
    }
    try {
      JWK jwk = JWK.parse(jwkJson);
      log.debug("JWK is valid: {} ", jwk.getKeyID());
      return true;
    } catch (Exception e) {

      log.warn("Invalid JWK {}, jwk: {}", e.getMessage(), jwkDto);
      return false;
    }
  }
}
