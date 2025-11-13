// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

class AccountMapperTest {


  AccountEntityMapper accountEntityMapper = new AccountEntityMapper();

  @Test
  void testToAccountEntity() {
    CreateAccountRequestDto requestDto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.businnes.se")
        .personalIdentityNumber("770101-1234")
        .telephoneNumber(Optional.of("070 123 123 12"))
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("22").build())
        .build();
    assertThat(accountEntityMapper.toAccountEntity(requestDto))
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.getId()).isNull();
          assertThat(account.getEmailAdress()).isEqualTo(requestDto.emailAdress());
          assertThat(account.getPublicKey()).isNotNull();
          assertThat(account.getPublicKey().getX()).isEqualTo(requestDto.publicKey().x());
        });
  }

  @Test
  void testToAccountEntityAllOptionalFieldsNull() {
    CreateAccountRequestDto requestDto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.businnes.se")
        .personalIdentityNumber("770101-1234")
        .telephoneNumber(Optional.empty())
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("22")
            .kid(null)
            .alg(null)
            .use(null)
            .build())
        .build();
    assertThat(accountEntityMapper.toAccountEntity(requestDto))
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.getTelephoneNumber()).isNull();
          assertThat(account.getPublicKey().getAlg()).isNull();
          assertThat(account.getPublicKey().getUse()).isNull();
          assertThat(account.getPublicKey().getKid()).isNull();
        });
  }

  @Test
  void testToDto() {
    AccountEntity entity = new AccountEntity(
        "770101-1234",
        "none@your.business.se",
        "070 123 123 12",
        TestUtils.generateJwkEntity("11"));
    entity.setId(UUID.randomUUID());
    AccountDto accountDto = accountEntityMapper.toAccountDto(entity);
    assertThat(accountDto)
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.id()).isNotNull();
          assertThat(account.emailAdress()).isEqualTo("none@your.business.se");
          assertThat(account.personalIdentityNumber()).isEqualTo("770101-1234");
          assertThat(account.telephoneNumber()).isPresent();
          assertThat(account.telephoneNumber().get()).contains("070 123 123 12");
          assertThat(account.publicKey()).isNotNull();
          assertThat(account.publicKey().x())
              .isEqualTo(TestUtils.generateJwkEntity("11").getX());
          assertThat(account.publicKey().kid()).isEqualTo("11");

        });
  }

  @Test
  void testToDtoAllOptionalFieldsNull() {
    AccountEntity entity = new AccountEntity(
        "770101-1234",
        "none@your.business.se",
        null,
        new PublicKeyEntity(
            "EC",
            null,
            null,
            null,
            "P-256",
            "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
            "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM"));
    entity.setId(UUID.randomUUID());
    AccountDto accountDto = accountEntityMapper.toAccountDto(entity);
    assertThat(accountDto)
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.telephoneNumber()).isEmpty();
          assertThat(account.publicKey()).isNotNull();
          assertThat(account.publicKey().kid()).isNull();
          assertThat(account.publicKey().alg()).isNull();
          assertThat(account.publicKey().use()).isNull();
        });
  }

}
