// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

import javax.sql.rowset.serial.SerialBlob;

public class AccountEntityMapperTest {

  private static AccountEntityMapper mapper;

  @BeforeAll
  static void startup() {
    mapper = new AccountEntityMapper();
  }

  @Test
  void assertThatToAccountEntity_nullAccountRequest_throwsException() {

    assertThrows(Exception.class, () -> mapper.toAccountEntity(null));
  }

  @Test
  void assertThatToAccountEntity_emptyPublicKey_throwsException() {

    var accountRequestDto = CreateAccountRequestDtoBuilder.builder()
      .build();

    assertThrows(Exception.class, () -> mapper.toAccountEntity(accountRequestDto));
  }

  @Test
  void assertThatToAccountEntity_emptyPersonalValues_shouldNotThrow() {

    var accountRequestDto = CreateAccountRequestDtoBuilder.builder()
      .publicKey(PublicKeyDtoBuilder.builder().build())
      .build();

    assertDoesNotThrow(() -> mapper.toAccountEntity(accountRequestDto));
  }

  @Test
  void assertThatToAccountEntity_accountValues_containsMappedEqualValues() {

    var expectedPersonalIdentityNumber = randomId();
    var expectedEmail = "test.testsson@test.xx";
    var expectedPhoneNumber = "0700000000";

    var accountRequestDto = CreateAccountRequestDtoBuilder.builder()
      .personalIdentityNumber(expectedPersonalIdentityNumber)
      .emailAdress(expectedEmail)
      .telephoneNumber(Optional.of(expectedPhoneNumber))
      .publicKey(PublicKeyDtoBuilder.builder().build())
      .build();

    var accountEntity = mapper.toAccountEntity(accountRequestDto);

    assertEquals(expectedPersonalIdentityNumber, accountEntity.getPersonalIdentityNumber());
    assertEquals(expectedEmail, accountEntity.getEmail());
    assertEquals(expectedPhoneNumber, accountEntity.getPhone());
  }

  @Test
  void assertThatToAccountEntity_deviceKeyValues_containsMappedValues() {

    var expectedKeyId = randomId();

    var accountRequestDto = CreateAccountRequestDtoBuilder.builder()
      .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(expectedKeyId).build())
      .build();

    var accountEntity = mapper.toAccountEntity(accountRequestDto);

    var actualDeviceKey = accountEntity.getDeviceKey();
    assertThat(actualDeviceKey).isNotNull();
    assertNotNull(actualDeviceKey.getKid());
    assertEquals(expectedKeyId, actualDeviceKey.getKid());
    assertNotNull(actualDeviceKey.getAlg());
    assertNotNull(actualDeviceKey.getUse());
    assertNotNull(actualDeviceKey.getKty());
    assertNotNull(actualDeviceKey.getCrv());
    assertNotNull(actualDeviceKey.getX());
    assertNotNull(actualDeviceKey.getY());
  }

  @Test
  void assertThatToExtendedAccountDto_nullAccountEntity_throwsException() {

    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(null));
  }

  @Test
  void assertThatToExtendedAccountDto_nullDeviceKey_throwsException() {

    var accountEntity = new AccountEntity();
    accountEntity.setWalletKey(new PublicKeyEntity());

    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(accountEntity));
  }

  @Test
  void assertThatToExtendedAccountDto_nullWalletKey_throwsException() {

    var accountEntity = new AccountEntity();
    accountEntity.setDeviceKey(new PublicKeyEntity());

    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(accountEntity));
  }

  @Test
  void assertThatToExtendedAccountEntity_accountValues_containsEqualMappedValues() {

    var expectedPersonalIdentityNumber = randomId();
    var expectedEmail = "test.testsson@test.xx";
    var expectedPhoneNumber = "0700000000";

    var accountEntity = new AccountEntity(
      expectedPersonalIdentityNumber,
      expectedEmail,
      expectedPhoneNumber,
      null,
      new PublicKeyEntity(),
      new PublicKeyEntity());

    var extendedAccountDto = mapper.toExtendedAccountDto(accountEntity);

    assertEquals(expectedPersonalIdentityNumber, extendedAccountDto.personalIdentityNumber());
    assertEquals(expectedEmail, extendedAccountDto.emailAdress());
    assertThat(extendedAccountDto.telephoneNumber()).isPresent();
    assertEquals(expectedPhoneNumber, extendedAccountDto.telephoneNumber().get());
  }

  @Test
  void assertThatToExtendedAccountEntity_nullDeviceKey_throwsException() {

    var accountEntity = new AccountEntity(
      null,
      null,
      null,
      null,
      new PublicKeyEntity(),
      null);

    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(accountEntity));
  }

  @Test
  void assertThatToExtendedAccountEntity_nullWalletKey_throwsException() {

    var accountEntity = new AccountEntity(
      null,
      null,
      null,
      null,
      null,
      new PublicKeyEntity());


    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(accountEntity));
  }

  @Test
  void assertThatToExtendedAccountEntity_deviceKeyValues_mappedToEqualKeyId() {

    var expectedKeyId = randomId();
    var publicKeyEntity = new PublicKeyEntity(
      randomId(),
      expectedKeyId,
      randomId(),
      randomId(),
      randomId(),
      randomId(),
      randomId()
    );
    var accountEntity = new AccountEntity(
      null,
      null,
      null,
      null,
      new PublicKeyEntity(),
      publicKeyEntity);

    var extendedAccountDto = mapper.toExtendedAccountDto(accountEntity);
    var actualKey = extendedAccountDto.deviceKey();

    assertThat(actualKey).isNotNull();
    assertNotNull(actualKey.kid());
    assertEquals(expectedKeyId, actualKey.kid());
    assertNotNull(actualKey.alg());
    assertNotNull(actualKey.use());
    assertNotNull(actualKey.kty());
    assertNotNull(actualKey.crv());
    assertNotNull(actualKey.x());
    assertNotNull(actualKey.y());
  }

  @Test
  void assertThatToExtendedAccountEntity_walletKeyValues_containsMappedValues() {

    var expectedKeyId = randomId();
    var publicKeyEntity = new PublicKeyEntity(
      randomId(),
      expectedKeyId,
      randomId(),
      randomId(),
      randomId(),
      randomId(),
      randomId()
    );
    var accountEntity = new AccountEntity(
      null,
      null,
      null,
      null,
      publicKeyEntity,
      new PublicKeyEntity());

    var extendedAccountDto = mapper.toExtendedAccountDto(accountEntity);
    var actualKey = extendedAccountDto.walletKey();

    assertThat(actualKey).isNotNull();
    assertNotNull(actualKey.kid());
    assertEquals(expectedKeyId, actualKey.kid());
    assertNotNull(actualKey.alg());
    assertNotNull(actualKey.use());
    assertNotNull(actualKey.kty());
    assertNotNull(actualKey.crv());
    assertNotNull(actualKey.x());
    assertNotNull(actualKey.y());
  }

  @Test
  void assertThatToExtendedAccountEntity_securityEnvelopeContent_containsMappedEqualContent() throws Exception {

    var expectedContent = randomId();
    byte[] bytes = expectedContent.getBytes(StandardCharsets.UTF_8);
    var blob = new SerialBlob(bytes);

    var accountEntity = new AccountEntity(
      null,
      null,
      null,
      blob,
      new PublicKeyEntity(),
      new PublicKeyEntity());

    var extendedAccountDto = mapper.toExtendedAccountDto(accountEntity);
    var actualContent = extendedAccountDto.securityEnvelope();

    assertThat(actualContent).isNotNull();
    assertEquals(expectedContent, actualContent);
  }


  @Test
  void assertThatToAccountDto_withNullAccountEntity_throwsException() {

    assertThrows(Exception.class, () -> mapper.toAccountDto(null));
  }

  @Test
  void assertThatToAccountDto_withNullKeys_throwsException() {

    var accountEntity = new AccountEntity();

    assertThrows(Exception.class, () -> mapper.toAccountDto(accountEntity));
  }

  @Test
  void assertThatToAccountDto_emptyPersonalValues_shouldNotThrow() {

    var accountEntity = new AccountEntity(
      null,
      null,
      null,
      null,
      new PublicKeyEntity(),
      new PublicKeyEntity()
    );

    assertDoesNotThrow(() -> mapper.toAccountDto(accountEntity));
  }

  @Test
  void assertThatToAccountDto_withPersonalAccountValues_containsMappedEqualValues() {

    var expectedPersonalIdentityNumber = randomId();
    var expectedEmail = "test.testsson@test.xx";
    var expectedPhoneNumber = "0700000000";

    var accountEntity = new AccountEntity(
      expectedPersonalIdentityNumber,
      expectedEmail,
      expectedPhoneNumber,
      null,
      new PublicKeyEntity(),
      new PublicKeyEntity()
    );

    var mappedEntity = mapper.toAccountDto(accountEntity);

    assertEquals(expectedPersonalIdentityNumber, mappedEntity.personalIdentityNumber());
    assertEquals(expectedEmail, mappedEntity.emailAdress());
    assertThat(mappedEntity.telephoneNumber()).isPresent();
    assertEquals(expectedPhoneNumber, mappedEntity.telephoneNumber().get());
  }

  @Test
  void assertThatToAccountDto_deviceKeyValues_containsMappedValues() {

    var expectedKeyId = randomId();
    var publicKeyEntity = new PublicKeyEntity(
      randomId(),
      expectedKeyId,
      randomId(),
      randomId(),
      randomId(),
      randomId(),
      randomId()
    );
    var accountEntity = new AccountEntity(
      null,
      null,
      null,
      null,
      new PublicKeyEntity(),
      publicKeyEntity
    );

    var mappedEntity = mapper.toAccountDto(accountEntity);

    var actualDeviceKey = mappedEntity.publicKey();
    assertThat(actualDeviceKey).isNotNull();
    assertNotNull(actualDeviceKey.kid());
    assertEquals(expectedKeyId, actualDeviceKey.kid());
    assertNotNull(actualDeviceKey.alg());
    assertNotNull(actualDeviceKey.use());
    assertNotNull(actualDeviceKey.kty());
    assertNotNull(actualDeviceKey.crv());
    assertNotNull(actualDeviceKey.x());
    assertNotNull(actualDeviceKey.y());
  }

  private static String randomId() {
    return UUID.randomUUID().toString();
  }
}
