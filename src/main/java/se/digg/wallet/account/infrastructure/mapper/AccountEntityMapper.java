// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import java.util.Optional;
import org.springframework.stereotype.Service;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.JwkDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.JwkEntity;

@Service
public class AccountEntityMapper {


  public AccountEntity toAccountEntity(CreateAccountRequestDto accountRequestDto) {
    return new AccountEntity(accountRequestDto.personalIdentityNumber(),
        accountRequestDto.emailAdress(),
        accountRequestDto.telephoneNumber().orElse(null),
        new JwkEntity(
            accountRequestDto.jwk().kty(),
            accountRequestDto.jwk().kid(),
            accountRequestDto.jwk().alg(),
            accountRequestDto.jwk().use(),
            accountRequestDto.jwk().crv(),
            accountRequestDto.jwk().x(),
            accountRequestDto.jwk().y()));
  }

  public AccountDto toAccountDto(AccountEntity accountEntity) {
    return AccountDtoBuilder.builder()
        .id(accountEntity.getId())
        .emailAdress(accountEntity.getEmailAdress())
        .personalIdentityNumber(accountEntity.getPersonalIdentityNumber())
        .telephoneNumber(Optional.of(accountEntity.getTelephoneNumber()))
        .jwk(JwkDtoBuilder.builder()
            .kty(accountEntity.getJwk().getKty())
            .kid(accountEntity.getJwk().getKid())
            .alg(accountEntity.getJwk().getAlg())
            .use(accountEntity.getJwk().getUse())
            .crv(accountEntity.getJwk().getCrv())
            .x(accountEntity.getJwk().getX())
            .y(accountEntity.getJwk().getY())
            .build())

        .build();


  }


}
