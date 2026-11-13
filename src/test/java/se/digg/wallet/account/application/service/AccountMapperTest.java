// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.domain.model.ExtendedAccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.mapper.JwkMapper;
import se.digg.wallet.account.infrastructure.model.WalletEntity;

class AccountMapperTest {


  AccountEntityMapper accountEntityMapper = new AccountEntityMapper();

  @Test
  void testToDeviceKey() {
    CreateAccountRequestDto requestDto = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("22").build())
        .build();
    assertThat(accountEntityMapper.toDeviceKey(requestDto))
        .isNotNull()
        .satisfies(deviceKey -> {
          assertThat(deviceKey.getKid()).isEqualTo("22");
          assertThat(deviceKey.getThumbprint()).isNotBlank();
          assertThat(TestUtils.parseJwk(deviceKey.getJwk()))
              .isEqualTo(TestUtils.expectedJwk(requestDto.publicKey()));
        });
  }

  @Test
  void testToDeviceKeyAllOptionalFieldsNull() {
    CreateAccountRequestDto requestDto = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("22")
            .kid(null)
            .alg(null)
            .use(null)
            .build())
        .build();
    assertThat(accountEntityMapper.toDeviceKey(requestDto))
        .isNotNull()
        .satisfies(deviceKey -> {
          assertThat(deviceKey.getKid()).isNull();
          assertThat(TestUtils.parseJwk(deviceKey.getJwk()))
              .isEqualTo(TestUtils.expectedJwk(requestDto.publicKey()));
        });
  }

  @Test
  void testToDto() {
    var deviceKey = TestUtils.generateJwkEntity("11");
    WalletEntity entity = TestUtils.wallet(null, deviceKey);
    ExtendedAccountDto accountDto = accountEntityMapper.toExtendedAccountDto(entity);
    assertThat(accountDto)
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.id()).isNotNull();
          assertThat(account.deviceKey()).isNotNull();
          assertThat(TestUtils.expectedJwk(account.deviceKey()))
              .isEqualTo(TestUtils.parseJwk(deviceKey.getJwk()));
          assertThat(account.deviceKey().kid()).isEqualTo("11");

        });
  }

  @Test
  void testToDtoAllOptionalFieldsNull() {
    WalletEntity entity = TestUtils.wallet(
        null,
        JwkMapper.toDeviceKey(TestUtils.publicKeyDtoBuilderWithDefaults(null)
            .alg(null)
            .use(null)
            .build()));
    ExtendedAccountDto accountDto = accountEntityMapper.toExtendedAccountDto(entity);
    assertThat(accountDto)
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.deviceKey()).isNotNull();
          assertThat(account.deviceKey().kid()).isNull();
          assertThat(account.deviceKey().alg()).isNull();
          assertThat(account.deviceKey().use()).isNull();
        });
  }

}
