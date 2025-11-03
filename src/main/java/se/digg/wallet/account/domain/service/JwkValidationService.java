// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.digg.wallet.account.application.model.JwkDto;

@Service
public class JwkValidationService {
  Logger log = LoggerFactory.getLogger(JwkValidationService.class);

  private final ObjectMapper objectMapper;

  public JwkValidationService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public boolean validateJwk(JwkDto jwkDto) {
    String jwkJson;
    try {
      jwkJson = objectMapper.writeValueAsString(jwkDto);
    } catch (JsonProcessingException e) {
      log.error("Unable to create json from internal object", e);
      return false;
    }
    try {
      JWK jwk = JWK.parse(jwkJson);
      log.debug("JWK is valid: {} ", jwk.getKeyID());
      return true;
    } catch (Exception e) {
      log.warn("Invalid JWK {}", e.getMessage());
      return false;
    }
  }
}
