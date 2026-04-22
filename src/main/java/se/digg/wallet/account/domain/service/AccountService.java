// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
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
        verifyUniquenessAndStore(accountRequestDto));
  }

  public Optional<AccountDto> getAccountById(UUID id) {
    return accountRepository.findById(id).map(accountEntityMapper::toAccountDto);
  }

  public PublicKeyDto createWalletKey(UUID accountId, PublicKeyDto walletKeyDto) {

    Map<String, Object> walletKey = new HashMap<>();
    walletKey.put("kty", walletKeyDto.kty());
    walletKey.put("kid", walletKeyDto.kid());
    walletKey.put("alg", walletKeyDto.alg());
    walletKey.put("use", walletKeyDto.use());
    walletKey.put("crv", walletKeyDto.crv());
    walletKey.put("x", walletKeyDto.x());
    walletKey.put("y", walletKeyDto.y());

    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    entity.setWalletKey(walletKey);
    var savedEntity = accountRepository.save(entity);
    return toPublicKeyDto(savedEntity.getWalletKey());
  }

  public List<PublicKeyDto> getWalletKeys(UUID accountId) {
    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    return List.of(toPublicKeyDto(entity.getWalletKey()));
  }

  public Optional<PublicKeyDto> getWalletKey(UUID accountId) {
    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    return Optional.of(toPublicKeyDto(entity.getWalletKey()));
  }

  public String createSecurityEnvelope(UUID accountId, String securityEnvelope) {
    AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
    entity.setSecurityEnvelope(securityEnvelope);
    var savedEntity = accountRepository.save(entity);
    return savedEntity.getSecurityEnvelope();
  }

  public Optional<String> getSecurityEnvelope(UUID accountId) {
    var entity = accountRepository.findById(accountId);
    return entity.map(AccountEntity::getSecurityEnvelope);
  }

  /*
   * public Optional<String> getSecurityEnvelope(UUID accountId, int enumType) { AccountEntity
   * entity = accountRepository.findById(accountId).orElseThrow(); return
   * Optional.of(entity.getSecurityEnvelope()); }
   */

  private AccountEntity verifyUniquenessAndStore(CreateAccountRequestDto accountRequestDto) {
    List<AccountEntity> entities =
        accountRepository.findByPersonalIdentityNumber(accountRequestDto.personalIdentityNumber());
    logger.debug("Incoming accountRequest: {}, found accounts {}", accountRequestDto, entities);
    if (!entities.isEmpty()) {
      logger.warn("Deleting duplicates (NON PRODUCTION CODE!!!): {}, {}", entities.size(),
          entities);
      accountRepository.deleteAll(entities);
    }
    AccountEntity storedEntity =
        accountRepository.save(accountEntityMapper.toAccountEntity(accountRequestDto));
    logger.debug("stored account {}", storedEntity);
    return storedEntity;

  }

  private PublicKeyDto toPublicKeyDto(Map<String, Object> wk) {
    return new PublicKeyDto(
        wk.get("kty").toString(),
        wk.get("kid").toString(),
        wk.get("alg").toString(),
        wk.get("use").toString(),
        wk.get("crv").toString(),
        wk.get("x").toString(),
        wk.get("y").toString());
  }
}
