package se.digg.wallet.account.domain.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;

@Component
public class AccountService {
  private final AccountRepository accountRepository;
  private final AccountEntityMapper accountEntityMapper;

  public AccountService(AccountRepository accountRepository,
      AccountEntityMapper accountEntityMapper) {
    this.accountRepository = accountRepository;
    this.accountEntityMapper = accountEntityMapper;
  }

  public AccountDto createAccount(CreateAccountRequestDto accountRequestDto) {
    return accountEntityMapper.toAccountDto(
        accountRepository.save(accountEntityMapper.toAccountEntity(accountRequestDto)));
  }

  public Optional<AccountDto> getAccountById(UUID id) {
    return accountRepository.findById(id).map(accountEntityMapper::toAccountDto);

  }
}
