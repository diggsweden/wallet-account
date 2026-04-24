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
import se.digg.wallet.account.domain.model.ExtendedAccountDto;
import se.digg.wallet.account.domain.model.ExtendedAccountDtoBuilder;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

@Service
public class AccountEntityMapper {


  public AccountEntity toAccountEntity(CreateAccountRequestDto accountRequestDto) {
    return new AccountEntity(accountRequestDto.personalIdentityNumber(),
        accountRequestDto.emailAdress(),
        accountRequestDto.telephoneNumber().orElse(null),
        null,
        null,
        new PublicKeyEntity(
            accountRequestDto.publicKey().kty(),
            accountRequestDto.publicKey().kid(),
            accountRequestDto.publicKey().alg(),
            accountRequestDto.publicKey().use(),
            accountRequestDto.publicKey().crv(),
            accountRequestDto.publicKey().x(),
            accountRequestDto.publicKey().y()));
  }

  public ExtendedAccountDto toExtendedAccountDto(AccountEntity accountEntity) {
    return ExtendedAccountDtoBuilder.builder()
        .id(accountEntity.getId())
        .emailAdress(accountEntity.getEmail())
        .personalIdentityNumber(accountEntity.getPersonalIdentityNumber())
        .telephoneNumber(Optional.ofNullable(accountEntity.getPhone()))
        .securityEnvelope(accountEntity.getSecurityEnvelope())
        .walletKey(PublicKeyDtoBuilder.builder()
            .kty(accountEntity.getWalletKey().getKty())
            .kid(accountEntity.getWalletKey().getKid())
            .alg(accountEntity.getWalletKey().getAlg())
            .use(accountEntity.getWalletKey().getUse())
            .crv(accountEntity.getWalletKey().getCrv())
            .x(accountEntity.getWalletKey().getX())
            .y(accountEntity.getWalletKey().getY())
            .build())
        .deviceKey(PublicKeyDtoBuilder.builder()
            .kty(accountEntity.getDeviceKey().getKty())
            .kid(accountEntity.getDeviceKey().getKid())
            .alg(accountEntity.getDeviceKey().getAlg())
            .use(accountEntity.getDeviceKey().getUse())
            .crv(accountEntity.getDeviceKey().getCrv())
            .x(accountEntity.getDeviceKey().getX())
            .y(accountEntity.getDeviceKey().getY())
            .build())
        .build();
  }

  public AccountDto toAccountDto(AccountEntity accountEntity) {
    return AccountDtoBuilder.builder()
        .id(accountEntity.getId())
        .emailAdress(accountEntity.getEmail())
        .personalIdentityNumber(accountEntity.getPersonalIdentityNumber())
        .telephoneNumber(Optional.ofNullable(accountEntity.getPhone()))
        .publicKey(PublicKeyDtoBuilder.builder()
            .kty(accountEntity.getDeviceKey().getKty())
            .kid(accountEntity.getDeviceKey().getKid())
            .alg(accountEntity.getDeviceKey().getAlg())
            .use(accountEntity.getDeviceKey().getUse())
            .crv(accountEntity.getDeviceKey().getCrv())
            .x(accountEntity.getDeviceKey().getX())
            .y(accountEntity.getDeviceKey().getY())
            .build())
        .build();
  }
}
