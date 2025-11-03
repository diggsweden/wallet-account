// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
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
        verifyUniqnessAndStore(accountRequestDto));
  }

  public Optional<AccountDto> getAccountById(UUID id) {
    return accountRepository.findById(id).map(accountEntityMapper::toAccountDto);

  }

  private AccountEntity verifyUniqnessAndStore(CreateAccountRequestDto accountRequestDto) {
    List<AccountEntity> entities =
        accountRepository.findByPersonalIdentityNumber(accountRequestDto.personalIdentityNumber());
    logger.debug("Incoming accountRequets: {}, found accounts {}", accountRequestDto, entities);
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
}
