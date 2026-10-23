// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.digg.wallet.account.application.exception.AccountAlreadyExistsException;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.mapper.BytesMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;

@Component
public class AccountService {
  Logger logger = LoggerFactory.getLogger(AccountService.class);
  private final AccountRepository accountRepository;
  private final AccountEntityMapper accountEntityMapper;

  public AccountService(AccountRepository accountRepository,
      AccountEntityMapper accountEntityMapper) {
    this.accountRepository = accountRepository;
    this.accountEntityMapper = accountEntityMapper;
  }

  public AccountDto createAccount(CreateAccountRequestDto accountRequestDto) {
    return accountEntityMapper.toAccountDto(
        storeAccountWithUniqueDeviceKey(accountRequestDto));
  }

  public Optional<AccountDto> getAccountById(UUID id) {
    return accountRepository.findById(id).map(accountEntityMapper::toAccountDto);
  }

  public String createHsmClientId(UUID accountId, String hsmClientId) {
    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    entity.setHsmClientId(hsmClientId);
    var savedEntity = accountRepository.save(entity);
    return savedEntity.getHsmClientId();
  }

  public Optional<String> getHsmClientId(UUID accountId) {
    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    return Optional.ofNullable(entity.getHsmClientId());
  }

  public String createSecurityEnvelope(UUID accountId, String securityEnvelope) {
    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    entity.setSecurityEnvelope(BytesMapper.stringToBytes(securityEnvelope));
    var savedEntity = accountRepository.save(entity);
    return BytesMapper.bytesToString(savedEntity.getSecurityEnvelope());
  }

  public Optional<String> getSecurityEnvelope(UUID accountId) {
    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    return Optional.ofNullable(BytesMapper.bytesToString(entity.getSecurityEnvelope()));
  }

  private AccountEntity storeAccountWithUniqueDeviceKey(CreateAccountRequestDto accountRequestDto) {

    AccountEntity entity = accountEntityMapper.toAccountEntity(accountRequestDto);
    var deviceKey = entity.getDeviceKey();

    if (accountRepository.existsByDeviceKeyKidOrDeviceKeyThumbprint(
        deviceKey.getKid(), deviceKey.getThumbprint())) {
      logger.warn("An account already exists for device key kid {} or thumbprint {}",
          deviceKey.getKid(), deviceKey.getThumbprint());
      throw new AccountAlreadyExistsException(
          "An account already exists for the provided device key.");
    }

    AccountEntity storedEntity = accountRepository.save(entity);
    logger.debug("stored account {}", storedEntity);
    return storedEntity;
  }

}
