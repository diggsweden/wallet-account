// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.sql.rowset.serial.SerialBlob;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  private static final String PERSONAL_IDENTITY_NUMBER = "191010101010";
  private static final String EMAIL = "test.testsson@test.xx";
  private static final String PHONE_NUMBER = "070-0000000";

  @Mock
  AccountRepository accountRepository;

  @Mock
  AccountEntityMapper accountEntityMapper;

  @InjectMocks
  AccountService accountService;

  @Test
  void assertThatCreateAccount_withNullUser_throwsException() {

    assertThrows(Exception.class, () -> accountService.createAccount(null));
  }

  @Test
  void assertThatCreateAccount_withRequiredValues_returnsCreatedAccountWithExpectedAccountId() {

    var expectedAccountId = UUID.randomUUID();

    var accountRequest = CreateAccountRequestDtoBuilder.builder().build();

    PublicKeyEntity deviceKey = new PublicKeyEntity();
    var createdAccountEntity = new AccountEntity();

    var expectedAccountDto = AccountDtoBuilder.builder()
      .id(expectedAccountId)
      .build();

    when(accountRepository.findByPersonalIdentityNumber(any())).thenReturn(Collections.emptyList());
    when(accountRepository.save(any())).thenReturn(createdAccountEntity);
    when(accountEntityMapper.toAccountDto(any())).thenReturn(expectedAccountDto);

    var createdAccountDto = accountService.createAccount(accountRequest);
    var actualAccountId = createdAccountDto.id();

    assertThat(actualAccountId).isEqualTo(expectedAccountId);
  }

  @Test
  void assertThatCreateAccount_usingExistingPersonalIdNbr_shouldDeleteExistingAccount() {

    var kid = randomId();

    var accountRequest = CreateAccountRequestDtoBuilder.builder().build();

    PublicKeyEntity deviceKey = new PublicKeyEntity();
    var existingAccountEntity = new AccountEntity();
    var createdAccountEntity = new AccountEntity();

    var expectedAccountDto = AccountDtoBuilder.builder()
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .emailAdress(EMAIL)
      .telephoneNumber(Optional.of(PHONE_NUMBER))
      .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(kid).build())
      .build();

    var existingAccountEntities = List.of(existingAccountEntity);
    when(accountRepository.findByPersonalIdentityNumber(any())).thenReturn(existingAccountEntities);
    when(accountRepository.save(any())).thenReturn(createdAccountEntity);
    when(accountEntityMapper.toAccountDto(any())).thenReturn(expectedAccountDto);

    var createdAccountDto = accountService.createAccount(accountRequest);

    assertThat(createdAccountDto).isNotNull();
    verify(accountRepository, times(1)).deleteAll(eq(existingAccountEntities));
  }

  @Test
  void assertThatGetAccountById_nonExistingAccountId_shouldReturnEmptyList() {

    when(accountRepository.findById(any())).thenReturn(Optional.empty());

    var actualAccount = accountService.getAccountById(UUID.randomUUID());

    assertThat(actualAccount).isEqualTo(Optional.empty());
  }

  @Test
  void assertThatGetAccountById_existingAccountId_shouldReturnExpectedAccount() {

    var expectedAccountId = UUID.randomUUID();

    var existingAccountEntity = new AccountEntity();

    var expectedAccountDto = AccountDtoBuilder.builder()
      .id(expectedAccountId)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .emailAdress(EMAIL)
      .telephoneNumber(Optional.of(PHONE_NUMBER))
      .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(randomId()).build())
      .build();

    when(accountRepository.findById(eq(expectedAccountId))).thenReturn(Optional.of(existingAccountEntity));
    when(accountEntityMapper.toAccountDto(any())).thenReturn(expectedAccountDto);

    var actualAccount = accountService.getAccountById(expectedAccountId);

    assertThat(actualAccount).isPresent();
  }

  @Test
  void assertThatCreateWalletKey_nonExistingAccount_throwsNoSuchElementException() {

    when(accountRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () ->
      accountService.createWalletKey(UUID.randomUUID(), PublicKeyDtoBuilder.builder().build()));
  }

  @Test
  void assertThatCreateWalletKey_addNewWalletKey_shouldReturnEqualKeyId() {

    var expectedKeyId = randomId();
    var walletKeyRequest = publicKeyDtoWithDefaults(expectedKeyId);

    var accountId = UUID.randomUUID();
    var existingAccountEntity = new AccountEntity(
      PERSONAL_IDENTITY_NUMBER,
      EMAIL,
      PHONE_NUMBER,
      null,
      null,
      new PublicKeyEntity()
    );
    existingAccountEntity.setId(accountId);

    when(accountRepository.findById(eq(accountId))).thenReturn(Optional.of(existingAccountEntity));
    when(accountRepository.save(eq(existingAccountEntity))).thenReturn(existingAccountEntity);

    var createdWalletKey = accountService.createWalletKey(accountId, walletKeyRequest);

    var actualKeyId = createdWalletKey.kid();
    assertThat(actualKeyId).isEqualTo(expectedKeyId);
  }

  @Test
  void assertThatCreateWalletKey_replaceExistingWalletKey_shouldReturnEqualKeyId() {

    var expectedKeyId = randomId();
    var walletKeyRequest = publicKeyDtoWithDefaults(expectedKeyId);

    var accountId = UUID.randomUUID();
    var existingAccountEntity = new AccountEntity(
      PERSONAL_IDENTITY_NUMBER,
      EMAIL,
      PHONE_NUMBER,
      null,
      new PublicKeyEntity(),
      new PublicKeyEntity()
    );
    existingAccountEntity.setId(accountId);

    when(accountRepository.findById(eq(accountId))).thenReturn(Optional.of(existingAccountEntity));
    when(accountRepository.save(eq(existingAccountEntity))).thenReturn(existingAccountEntity);

    var createdWalletKey = accountService.createWalletKey(accountId, walletKeyRequest);

    var actualKeyId = createdWalletKey.kid();
    assertThat(actualKeyId).isEqualTo(expectedKeyId);
  }

  @Test
  void assertThatGetWalletKey_nonExistingAccount_throwsNoSuchElementException() {

    when(accountRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> accountService.getWalletKey(UUID.randomUUID()));
  }

  @Test
  void assertThatGetWalletKey_existingAccountWithoutWalletKey_shouldReturnEmpty() {

    var expectedKeyId = randomId();
    var existingAccountEntity = new AccountEntity(
      PERSONAL_IDENTITY_NUMBER,
      EMAIL,
      PHONE_NUMBER,
      null,
      null,
      new PublicKeyEntity()
    );
    when(accountRepository.findById(any())).thenReturn(Optional.of(existingAccountEntity));

    var existingWalletKey = accountService.getWalletKey(UUID.randomUUID());
    assertThat(existingWalletKey).isEmpty();
  }

  @Test
  void assertThatGetWalletKey_existingAccountWithWalletKey_shouldReturnExpectedWalletKey() {

    var expectedKeyId = randomId();
    var existingAccountEntity = new AccountEntity();
    var existingWalletKey = new PublicKeyEntity(
      null,
      expectedKeyId,
      null,
      null,
      null,
      null,
      null
    );
    existingAccountEntity.setWalletKey(existingWalletKey);

    when(accountRepository.findById(any())).thenReturn(Optional.of(existingAccountEntity));

    var actualWalletKey = accountService.getWalletKey(UUID.randomUUID());
    assertThat(actualWalletKey).isPresent();

    var actualKeyId = actualWalletKey.get().kid();
    assertThat(actualKeyId).isEqualTo(expectedKeyId);
  }

  @Test
  void assertThatCreateSecurityEnvelope_nonExistingAccount_throwsNoSuchElementException() {

    when(accountRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () ->
      accountService.createSecurityEnvelope(UUID.randomUUID(), randomId()));
  }

  @Test
  void assertThatCreateSecurityEnvelope_addNewContent_shouldReturnCreatedContent() {

    var expectedContent = randomId();
    var existingAccountEntity = new AccountEntity();

    when(accountRepository.findById(any())).thenReturn(Optional.of(existingAccountEntity));
    when(accountRepository.save(eq(existingAccountEntity))).thenReturn(existingAccountEntity);

    var actualContent = accountService.createSecurityEnvelope(UUID.randomUUID(), expectedContent);

    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  void assertThatCreateSecurityEnvelope_replaceExistingContent_shouldReturnReplacedContent() throws Exception {

    var expectedContent = randomId();
    byte[] bytes = expectedContent.getBytes(StandardCharsets.UTF_8);
    var blob = new SerialBlob(bytes);

    var existingAccountEntity = new AccountEntity();
    existingAccountEntity.setSecurityEnvelope(blob);

    when(accountRepository.findById(any())).thenReturn(Optional.of(existingAccountEntity));
    when(accountRepository.save(eq(existingAccountEntity))).thenReturn(existingAccountEntity);

    var actualContent = accountService.createSecurityEnvelope(UUID.randomUUID(), expectedContent);

    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  void assertThatGetSecurityEnvelope_nonExistingAccount_throwsNoSuchElementException() {

    when(accountRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> accountService.getSecurityEnvelope(UUID.randomUUID()));
  }

  @Test
  void assertThatGetSecurityEnvelope_existingAccountWithoutSecurityEnvelope_shouldReturnEmpty() {

    var existingAccountEntity = new AccountEntity();

    when(accountRepository.findById(any())).thenReturn(Optional.of(existingAccountEntity));

    var actualContent = accountService.getSecurityEnvelope(UUID.randomUUID());
    assertThat(actualContent).isEmpty();
  }

  @Test
  void assertThatGetSecurityEnvelope_existingAccountWithSecurityEnvelope_shouldReturnExpectedContent() throws Exception {

    var expectedContent = randomId();
    byte[] bytes = expectedContent.getBytes(StandardCharsets.UTF_8);
    var blob = new SerialBlob(bytes);

    var existingAccountEntity = new AccountEntity();
    existingAccountEntity.setSecurityEnvelope(blob);

    when(accountRepository.findById(any())).thenReturn(Optional.of(existingAccountEntity));

    var actualContent = accountService.getSecurityEnvelope(UUID.randomUUID());
    assertThat(actualContent).isPresent();
    assertThat(actualContent.get()).isEqualTo(expectedContent);
  }

  private static String randomId() {
    return UUID.randomUUID().toString();
  }

  private static PublicKeyDto publicKeyDtoWithDefaults(String kid) {
    return PublicKeyDtoBuilder.builder()
      .kty("EC")
      .crv("P-256")
      .x("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4")
      .y("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM")
      .alg("alg")
      .use("enc")
      .kid(kid)
      .build();
  }
}
