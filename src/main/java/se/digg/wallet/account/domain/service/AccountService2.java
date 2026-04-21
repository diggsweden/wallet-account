package se.digg.wallet.account.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.model.AccountDto;

@Component
public class AccountService2 {

  public AccountService2() {}

  public AccountDto createAccount(CreateAccountRequestDto accountRequestDto) {
    return null;
  }

  public Optional<AccountDto> getAccountById(UUID id) {
    return null;
  }

  public AccountDto createWalletKeys(UUID accountId, List<PublicKeyDto> walletKeys) {
    return null;
  }

  public List<PublicKeyDto> getWalletKeys(UUID accountId) {
    return null;
  }

  public Optional<PublicKeyDto> getWalletKey(UUID accountId) {
    return null;
  }

  public AccountDto createSecurityEnvelopes(UUID accountId, List<String> securityEnvelopes) {
    return null;
  }

  public List<String> getSecurityEnvelopes(UUID accountId) {
    return null;
  }

  public Optional<String> getSecurityEnvelope(UUID accountId, int enumType) {
    return null;
  }
}
