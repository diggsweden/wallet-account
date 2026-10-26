// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
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
import se.digg.wallet.account.application.exception.AccountAlreadyExistsException;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;
import se.digg.wallet.account.infrastructure.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock
  AccountRepository accountRepository;

  @Mock
  WalletRepository walletRepository;

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

    var accountRequest = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();

    var createdAccountEntity = new AccountEntity();
    createdAccountEntity.setId(expectedAccountId);

    var expectedAccountDto = AccountDtoBuilder.builder()
        .id(expectedAccountId)
        .build();

    when(accountEntityMapper.toDeviceKey(any()))
        .thenReturn(TestUtils.generateJwkEntity(randomId()));
    when(walletRepository.existsByDeviceKeyKidOrDeviceKeyThumbprint(any(), any()))
        .thenReturn(false);
    when(accountRepository.save(any())).thenReturn(createdAccountEntity);
    when(walletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(accountEntityMapper.toAccountDto(any())).thenReturn(expectedAccountDto);

    var createdAccountDto = accountService.createAccount(accountRequest);
    var actualAccountId = createdAccountDto.id();

    assertThat(actualAccountId).isEqualTo(expectedAccountId);
  }

  @Test
  void assertThatCreateAccount_usingExistingDeviceKey_throwsAndDoesNotStore() {

    var kid = randomId();

    var accountRequest = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(kid).build())
        .build();

    var deviceKey = TestUtils.generateJwkEntity(kid);
    when(accountEntityMapper.toDeviceKey(any())).thenReturn(deviceKey);
    when(walletRepository.existsByDeviceKeyKidOrDeviceKeyThumbprint(
        eq(kid), eq(deviceKey.getThumbprint()))).thenReturn(true);

    assertThrows(AccountAlreadyExistsException.class,
        () -> accountService.createAccount(accountRequest));

    verify(accountRepository, never()).save(any());
    verify(walletRepository, never()).save(any());
  }

  @Test
  void assertThatGetAccountById_nonExistingAccountId_shouldReturnEmptyList() {

    when(walletRepository.findByAccountId(any())).thenReturn(List.of());

    var actualAccount = accountService.getAccountById(UUID.randomUUID());

    assertThat(actualAccount).isEqualTo(Optional.empty());
  }

  @Test
  void assertThatGetAccountById_existingAccountId_shouldReturnExpectedAccount() {

    var expectedAccountId = UUID.randomUUID();

    var existingWallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));

    var expectedAccountDto = AccountDtoBuilder.builder()
        .id(expectedAccountId)
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();

    when(walletRepository.findByAccountId(eq(expectedAccountId)))
        .thenReturn(List.of(existingWallet));
    when(accountEntityMapper.toAccountDto(any())).thenReturn(expectedAccountDto);

    var actualAccount = accountService.getAccountById(expectedAccountId);

    assertThat(actualAccount).isPresent();
  }



  @Test
  void assertThatCreateSecurityEnvelope_nonExistingAccount_throwsNoSuchElementException() {

    when(walletRepository.findByAccountId(any())).thenReturn(List.of());

    assertThrows(NoSuchElementException.class,
        () -> accountService.createSecurityEnvelope(UUID.randomUUID(), randomId()));
  }

  @Test
  void assertThatCreateSecurityEnvelope_addNewContent_shouldReturnCreatedContent() {

    var expectedContent = randomId();
    var existingWallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));
    when(walletRepository.save(eq(existingWallet))).thenReturn(existingWallet);

    var actualContent = accountService.createSecurityEnvelope(UUID.randomUUID(), expectedContent);

    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  void assertThatCreateSecurityEnvelope_replaceExistingContent_shouldReturnReplacedContent()
      throws Exception {

    var expectedContent = randomId();
    byte[] bytes = expectedContent.getBytes(StandardCharsets.UTF_8);
    var blob = bytes;

    var existingWallet = TestUtils.wallet(blob, TestUtils.generateJwkEntity(randomId()));

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));
    when(walletRepository.save(eq(existingWallet))).thenReturn(existingWallet);

    var actualContent = accountService.createSecurityEnvelope(UUID.randomUUID(), expectedContent);

    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  void assertThatGetSecurityEnvelope_nonExistingAccount_throwsNoSuchElementException() {

    when(walletRepository.findByAccountId(any())).thenReturn(List.of());

    assertThrows(NoSuchElementException.class,
        () -> accountService.getSecurityEnvelope(UUID.randomUUID()));
  }

  @Test
  void assertThatGetSecurityEnvelope_existingAccountWithoutSecurityEnvelope_shouldReturnEmpty() {

    var existingWallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));

    var actualContent = accountService.getSecurityEnvelope(UUID.randomUUID());
    assertThat(actualContent).isEmpty();
  }

  @Test
  void assertThatGetSecurityEnvelope_existingAccountWithSecurityEnvelope_shouldReturnExpectedContent()
      throws Exception {

    var expectedContent = randomId();
    byte[] bytes = expectedContent.getBytes(StandardCharsets.UTF_8);
    var blob = bytes;

    var existingWallet = TestUtils.wallet(blob, TestUtils.generateJwkEntity(randomId()));

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));

    var actualContent = accountService.getSecurityEnvelope(UUID.randomUUID());
    assertThat(actualContent).isPresent();
    assertThat(actualContent.get()).isEqualTo(expectedContent);
  }

  @Test
  void assertThatCreateHsmClientId_nonExistingAccount_throwsNoSuchElementException() {

    when(walletRepository.findByAccountId(any())).thenReturn(List.of());

    assertThrows(NoSuchElementException.class,
        () -> accountService.createHsmClientId(UUID.randomUUID(), randomId()));
  }

  @Test
  void assertThatCreateHsmClientId_addNewClientId_shouldReturnCreatedClientId() {

    var expectedClientId = randomId();
    var existingWallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));
    when(walletRepository.save(eq(existingWallet))).thenReturn(existingWallet);

    var actualClientId = accountService.createHsmClientId(UUID.randomUUID(), expectedClientId);

    assertThat(actualClientId).isEqualTo(expectedClientId);
  }

  @Test
  void assertThatCreateHsmClientId_replaceExistingClientId_shouldReturnReplacedClientId() {

    var expectedClientId = randomId();
    var existingWallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));
    existingWallet.setHsmClientId(randomId());

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));
    when(walletRepository.save(eq(existingWallet))).thenReturn(existingWallet);

    var actualClientId = accountService.createHsmClientId(UUID.randomUUID(), expectedClientId);

    assertThat(actualClientId).isEqualTo(expectedClientId);
  }

  @Test
  void assertThatGetHsmClientId_nonExistingAccount_throwsNoSuchElementException() {

    when(walletRepository.findByAccountId(any())).thenReturn(List.of());

    assertThrows(NoSuchElementException.class,
        () -> accountService.getHsmClientId(UUID.randomUUID()));
  }

  @Test
  void assertThatGetHsmClientId_existingAccountWithoutHsmClientId_shouldReturnEmpty() {

    var existingWallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));

    var actualClientId = accountService.getHsmClientId(UUID.randomUUID());
    assertThat(actualClientId).isEmpty();
  }

  @Test
  void assertThatGetHsmClientId_existingAccountWithHsmClientId_shouldReturnExpectedClientId() {

    var expectedClientId = randomId();
    var existingWallet = TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()));
    existingWallet.setHsmClientId(expectedClientId);

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(existingWallet));

    var actualClientId = accountService.getHsmClientId(UUID.randomUUID());
    assertThat(actualClientId).isPresent();
    assertThat(actualClientId.get()).isEqualTo(expectedClientId);
  }

  @Test
  void assertThatGetSecurityEnvelope_accountWithSeveralWallets_throwsIllegalStateException() {

    when(walletRepository.findByAccountId(any())).thenReturn(List.of(
        TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId())),
        TestUtils.wallet(null, TestUtils.generateJwkEntity(randomId()))));

    assertThrows(IllegalStateException.class,
        () -> accountService.getSecurityEnvelope(UUID.randomUUID()));
  }

  @Test
  void assertThatCreateAccount_storesAccountAndWalletWithDeviceKey() {

    var deviceKey = TestUtils.generateJwkEntity(randomId());
    var accountRequest = CreateAccountRequestDtoBuilder.builder()
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(deviceKey.getKid()).build())
        .build();
    var createdAccountEntity = new AccountEntity();
    createdAccountEntity.setId(UUID.randomUUID());

    when(accountEntityMapper.toDeviceKey(any())).thenReturn(deviceKey);
    when(accountRepository.save(any())).thenReturn(createdAccountEntity);
    when(walletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    accountService.createAccount(accountRequest);

    verify(walletRepository).save(argThat(wallet -> wallet.getAccount() == createdAccountEntity
        && wallet.getDeviceKey() == deviceKey));
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
