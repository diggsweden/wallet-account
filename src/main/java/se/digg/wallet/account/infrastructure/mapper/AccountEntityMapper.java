package se.digg.wallet.account.infrastructure.mapper;

import java.util.Optional;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

public class AccountEntityMapper {


  public AccountEntity toAccountEntity(CreateAccountRequestDto accountRequestDto) {
    return new AccountEntity(accountRequestDto.personalIdentityNumber(),
        accountRequestDto.emailAdress(),
        accountRequestDto.telephoneNumber().orElse(null),
        new PublicKeyEntity(
            accountRequestDto.publicKey().publicKeyBase64(),
            accountRequestDto.publicKey().publicKeyIdentifier()));
  }

  public AccountDto toAccountDto(AccountEntity accountEntity) {
    return AccountDtoBuilder.builder()
        .emailAdress(accountEntity.getEmailAdress())
        .personalIdentityNumber(accountEntity.getPersonalIdentityNumber())
        .telephoneNumber(Optional.of(accountEntity.getTelephoneNumber()))
        .publicKey(new PublicKeyDto(
            accountEntity.getPublicKey().getPublicKeyIdentifier(),
            accountEntity.getPublicKey().getPublicKey()))
        .build();


  }


}
