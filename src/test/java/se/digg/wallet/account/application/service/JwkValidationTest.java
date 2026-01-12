// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.service.JwkValidationService;
import tools.jackson.databind.ObjectMapper;

class JwkValidationTest {

  ObjectMapper objectMapper = new ObjectMapper();
  JwkValidationService jwkValidationService = new JwkValidationService(objectMapper);

  @Test
  void testJwkValidation() {
    assertThat(jwkValidationService.validateJwk(
        TestUtils.publicKeyDtoBuilderWithDefaults("11").build())).isTrue();
  }

  @Test
  void validateBadJwkTest() {
    PublicKeyDto badJwkDto = TestUtils
        .publicKeyDtoBuilderWithDefaults("999")
        .x("dummykey")
        .build();
    assertThat(jwkValidationService.validateJwk(badJwkDto)).isFalse();
  }
}
