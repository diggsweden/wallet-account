// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.domain.model.ExtendedAccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.mapper.JwkMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;

class AccountMapperTest {


  AccountEntityMapper accountEntityMapper = new AccountEntityMapper();

  @Test
  void testToAccountEntity() {
    CreateAccountRequestDto requestDto = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("22").build())
        .build();
    assertThat(accountEntityMapper.toAccountEntity(requestDto))
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.getId()).isNull();
          assertThat(account.getDeviceKey()).isNotNull();
          assertThat(account.getDeviceKey().getKid()).isEqualTo("22");
          assertThat(account.getDeviceKey().getThumbprint()).isNotBlank();
          assertThat(TestUtils.parseJwk(account.getDeviceKey().getJwk()))
              .isEqualTo(TestUtils.expectedJwk(requestDto.publicKey()));
        });
  }

  @Test
  void testToAccountEntityAllOptionalFieldsNull() {
    CreateAccountRequestDto requestDto = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("22")
            .kid(null)
            .alg(null)
            .use(null)
            .build())
        .build();
    assertThat(accountEntityMapper.toAccountEntity(requestDto))
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.getDeviceKey().getKid()).isNull();
          assertThat(TestUtils.parseJwk(account.getDeviceKey().getJwk()))
              .isEqualTo(TestUtils.expectedJwk(requestDto.publicKey()));
        });
  }

  @Test
  void testToDto() {
    var deviceKey = TestUtils.generateJwkEntity("11");
    AccountEntity entity = new AccountEntity(null, deviceKey);
    entity.setId(UUID.randomUUID());
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
    AccountEntity entity = new AccountEntity(
        null,
        JwkMapper.toDeviceKey(TestUtils.publicKeyDtoBuilderWithDefaults(null)
            .alg(null)
            .use(null)
            .build()));
    entity.setId(UUID.randomUUID());
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
