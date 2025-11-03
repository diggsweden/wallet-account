// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import java.util.Optional;
import org.springframework.stereotype.Service;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

@Service
public class AccountEntityMapper {


  public AccountEntity toAccountEntity(CreateAccountRequestDto accountRequestDto) {
    return new AccountEntity(accountRequestDto.personalIdentityNumber(),
        accountRequestDto.emailAdress(),
        accountRequestDto.telephoneNumber().orElse(null),
        new PublicKeyEntity(
            accountRequestDto.publicKey().kty(),
            accountRequestDto.publicKey().kid(),
            accountRequestDto.publicKey().alg(),
            accountRequestDto.publicKey().use(),
            accountRequestDto.publicKey().crv(),
            accountRequestDto.publicKey().x(),
            accountRequestDto.publicKey().y()));
  }

  public AccountDto toAccountDto(AccountEntity accountEntity) {
    return AccountDtoBuilder.builder()
        .id(accountEntity.getId())
        .emailAdress(accountEntity.getEmailAdress())
        .personalIdentityNumber(accountEntity.getPersonalIdentityNumber())
        .telephoneNumber(Optional.of(accountEntity.getTelephoneNumber()))
        .publicKey(PublicKeyDtoBuilder.builder()
            .kty(accountEntity.getPublicKey().getKty())
            .kid(accountEntity.getPublicKey().getKid())
            .alg(accountEntity.getPublicKey().getAlg())
            .use(accountEntity.getPublicKey().getUse())
            .crv(accountEntity.getPublicKey().getCrv())
            .x(accountEntity.getPublicKey().getX())
            .y(accountEntity.getPublicKey().getY())
            .build())

        .build();


  }


}
