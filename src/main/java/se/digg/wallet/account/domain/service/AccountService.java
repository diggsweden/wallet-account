// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.digg.wallet.account.application.exception.WalletAccountException;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.mapper.BlobMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;
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

    PublicKeyEntity walletKey = new PublicKeyEntity(
        walletKeyDto.kty(),
        walletKeyDto.kid(),
        walletKeyDto.alg(),
        walletKeyDto.use(),
        walletKeyDto.crv(),
        walletKeyDto.x(),
        walletKeyDto.y());

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
    try {
      AccountEntity entity = accountRepository.findById(accountId).orElseThrow();
      entity.setSecurityEnvelope(BlobMapper.stringToBlob(securityEnvelope));
      var savedEntity = accountRepository.save(entity);
      return BlobMapper.blobToString(savedEntity.getSecurityEnvelope());
    } catch (SQLException ex) {
      // TODO: better exception handling
      throw new WalletAccountException(ex.getMessage());
    }
  }

  public Optional<String> getSecurityEnvelope(UUID accountId) {
    try {
      var entity = accountRepository.findById(accountId);
      return Optional.ofNullable(BlobMapper.blobToString(
          entity.map(AccountEntity::getSecurityEnvelope).get()));
    } catch (SQLException ex) {
      // TODO: better exception handling
      throw new WalletAccountException(ex.getMessage());
    }
  }

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

  private PublicKeyDto toPublicKeyDto(PublicKeyEntity wk) {
    return new PublicKeyDto(
        wk.getKty(),
        wk.getKid(),
        wk.getAlg(),
        wk.getUse(),
        wk.getCrv(),
        wk.getX(),
        wk.getY());
  }
}
