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
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
import se.digg.wallet.account.infrastructure.model.WalletEntity;


public class AccountEntityMapperTest {

  private static AccountEntityMapper mapper;

  @BeforeAll
  static void startup() {
    mapper = new AccountEntityMapper();
  }

  @Test
  void assertThatToDeviceKey_nullAccountRequest_throwsException() {

    assertThrows(Exception.class, () -> mapper.toDeviceKey(null));
  }

  @Test
  void assertThatToDeviceKey_emptyPublicKey_throwsException() {

    var accountRequestDto = CreateAccountRequestDtoBuilder.builder()
        .build();

    assertThrows(Exception.class, () -> mapper.toDeviceKey(accountRequestDto));
  }

  @Test
  void assertThatToDeviceKey_publicKeyWithoutMembers_throwsException() {

    var accountRequestDto = CreateAccountRequestDtoBuilder.builder()
        .publicKey(PublicKeyDtoBuilder.builder().build())
        .build();

    assertThrows(IllegalArgumentException.class, () -> mapper.toDeviceKey(accountRequestDto));
  }


  @Test
  void assertThatToDeviceKey_deviceKeyValues_containsMappedValues() {

    var expectedKeyId = randomId();

    var accountRequestDto = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(expectedKeyId).build())
        .build();

    var actualDeviceKey = mapper.toDeviceKey(accountRequestDto);
    assertThat(actualDeviceKey).isNotNull();
    assertNotNull(actualDeviceKey.getKid());
    assertEquals(expectedKeyId, actualDeviceKey.getKid());
    assertNotNull(actualDeviceKey.getThumbprint());
    assertEquals(TestUtils.expectedJwk(accountRequestDto.publicKey()),
        TestUtils.parseJwk(actualDeviceKey.getJwk()));
  }

  @Test
  void assertThatToExtendedAccountDto_nullWallet_throwsException() {

    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(null));
  }

  @Test
  void assertThatToExtendedAccountDto_nullDeviceKey_throwsException() {

    var wallet = new WalletEntity();

    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(wallet));
  }



  @Test
  void assertThatToExtendedAccountEntity_nullDeviceKey_throwsException() {

    var wallet = TestUtils.wallet(null, null);

    assertThrows(Exception.class, () -> mapper.toExtendedAccountDto(wallet));
  }


  @Test
  void assertThatToExtendedAccountEntity_deviceKeyValues_mappedToEqualKeyId() {

    var expectedKeyId = randomId();
    var publicKeyEntity = JwkMapper.toDeviceKey(
        TestUtils.publicKeyDtoBuilderWithDefaults(expectedKeyId).build());
    var wallet = TestUtils.wallet(null, publicKeyEntity);

    var extendedAccountDto = mapper.toExtendedAccountDto(wallet);
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
  void assertThatToExtendedAccountEntity_securityEnvelopeContent_containsMappedEqualContent()
      throws Exception {

    var expectedContent = randomId();
    byte[] bytes = expectedContent.getBytes(StandardCharsets.UTF_8);
    var blob = bytes;

    var wallet = TestUtils.wallet(blob, TestUtils.generateJwkEntity(randomId()));

    var extendedAccountDto = mapper.toExtendedAccountDto(wallet);
    var actualContent = extendedAccountDto.securityEnvelope();

    assertThat(actualContent).isNotNull();
    assertEquals(expectedContent, actualContent);
  }


  @Test
  void assertThatToAccountDto_withNullWallet_throwsException() {

    assertThrows(Exception.class, () -> mapper.toAccountDto(null));
  }

  @Test
  void assertThatToAccountDto_withNullKeys_throwsException() {

    var wallet = new WalletEntity();

    assertThrows(Exception.class, () -> mapper.toAccountDto(wallet));
  }

  @Test
  void assertThatToAccountDto_emptyPersonalValues_shouldNotThrow() {

    var wallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));

    assertDoesNotThrow(() -> mapper.toAccountDto(wallet));
  }


  @Test
  void assertThatToAccountDto_deviceKeyValues_containsMappedValues() {

    var expectedKeyId = randomId();
    var publicKeyEntity = JwkMapper.toDeviceKey(
        TestUtils.publicKeyDtoBuilderWithDefaults(expectedKeyId).build());
    var wallet = TestUtils.wallet(null, publicKeyEntity);

    var mappedEntity = mapper.toAccountDto(wallet);

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
