// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.digg.wallet.account.application.exception.AccountAlreadyExistsException;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.mapper.BytesMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.WalletEntity;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;
import se.digg.wallet.account.infrastructure.repository.WalletRepository;

@Component
public class AccountService {
  Logger logger = LoggerFactory.getLogger(AccountService.class);
  private final AccountRepository accountRepository;
  private final WalletRepository walletRepository;
  private final AccountEntityMapper accountEntityMapper;

  public AccountService(AccountRepository accountRepository, WalletRepository walletRepository,
      AccountEntityMapper accountEntityMapper) {
    this.accountRepository = accountRepository;
    this.walletRepository = walletRepository;
    this.accountEntityMapper = accountEntityMapper;
  }

  @Transactional
  public AccountDto createAccount(CreateAccountRequestDto accountRequestDto) {
    return accountEntityMapper.toAccountDto(
        storeAccountWithUniqueDeviceKey(accountRequestDto));
  }

  public Optional<AccountDto> getAccountById(UUID id) {
    List<WalletEntity> wallets = walletRepository.findByAccountId(id);
    if (wallets.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(accountEntityMapper.toAccountDto(singleWallet(id, wallets)));
  }

  public String createHsmClientId(UUID accountId, String hsmClientId) {
    WalletEntity wallet = singleWallet(accountId);
    wallet.setHsmClientId(hsmClientId);
    return walletRepository.save(wallet).getHsmClientId();
  }

  public Optional<String> getHsmClientId(UUID accountId) {
    return Optional.ofNullable(singleWallet(accountId).getHsmClientId());
  }

  public String createSecurityEnvelope(UUID accountId, String securityEnvelope) {
    WalletEntity wallet = singleWallet(accountId);
    wallet.setSecurityEnvelope(BytesMapper.stringToBytes(securityEnvelope));
    return BytesMapper.bytesToString(walletRepository.save(wallet).getSecurityEnvelope());
  }

  public Optional<String> getSecurityEnvelope(UUID accountId) {
    return Optional.ofNullable(
        BytesMapper.bytesToString(singleWallet(accountId).getSecurityEnvelope()));
  }

  private WalletEntity storeAccountWithUniqueDeviceKey(CreateAccountRequestDto accountRequestDto) {

    var deviceKey = accountEntityMapper.toDeviceKey(accountRequestDto);

    if (walletRepository.existsByDeviceKeyKidOrDeviceKeyThumbprint(
        deviceKey.getKid(), deviceKey.getThumbprint())) {
      logger.warn("An account already exists for device key kid {} or thumbprint {}",
          deviceKey.getKid(), deviceKey.getThumbprint());
      throw new AccountAlreadyExistsException(
          "An account already exists for the provided device key.");
    }

    AccountEntity account = accountRepository.save(new AccountEntity());
    WalletEntity wallet = walletRepository.save(new WalletEntity(account, deviceKey, null));
    logger.debug("stored account {} with wallet {}", account, wallet);
    return wallet;
  }

  /**
   * The account's only wallet. The account-level endpoints assume exactly one wallet per account
   * until the wallet API decides how they behave with several.
   *
   * @throws NoSuchElementException if the account has no wallet, i.e. does not exist
   */
  private WalletEntity singleWallet(UUID accountId) {
    return singleWallet(accountId, walletRepository.findByAccountId(accountId));
  }

  private static WalletEntity singleWallet(UUID accountId, List<WalletEntity> wallets) {
    if (wallets.isEmpty()) {
      throw new NoSuchElementException("No wallet for account " + accountId);
    }
    if (wallets.size() > 1) {
      throw new IllegalStateException(
          "Expected exactly one wallet for account " + accountId + " but found " + wallets.size());
    }
    return wallets.getFirst();
  }

}
