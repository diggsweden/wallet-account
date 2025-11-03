// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.JwkDto;
import se.digg.wallet.account.application.model.JwkDtoBuilder;
import se.digg.wallet.account.domain.service.JwkValidationService;

class JwkValidationTest {

  ObjectMapper objectMapper = new ObjectMapper();
  JwkValidationService jwkValidationService = new JwkValidationService(objectMapper);

  @Test
  void testJwkValidation() {
    assertThat(jwkValidationService.validateJwk(TestUtils.generateJwkDto("11"))).isTrue();
  }

  @Test
  void validateBadJwkTest() {
    JwkDto badJwkDto = JwkDtoBuilder.builder()
        .kty("EC")
        .crv("P-256")
        .x("MKBCTNI7Tu6KPAqv7D4")
        .y("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM")
        .alg("alg")
        .use("enc")
        .kid("kid")
        .build();
    assertThat(jwkValidationService.validateJwk(badJwkDto)).isFalse();
  }
}
