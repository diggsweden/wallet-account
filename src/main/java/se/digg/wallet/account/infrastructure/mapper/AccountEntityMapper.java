// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import org.springframework.stereotype.Service;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.domain.model.ExtendedAccountDto;
import se.digg.wallet.account.domain.model.ExtendedAccountDtoBuilder;
import se.digg.wallet.account.infrastructure.model.AccountEntity;

@Service
public class AccountEntityMapper {


  public AccountEntity toAccountEntity(CreateAccountRequestDto accountRequestDto) {
    return new AccountEntity(
        null,
        JwkMapper.toDeviceKey(accountRequestDto.publicKey()));
  }

  public ExtendedAccountDto toExtendedAccountDto(AccountEntity accountEntity) {
    return ExtendedAccountDtoBuilder.builder()
        .id(accountEntity.getId())
        .securityEnvelope(BytesMapper.bytesToString(accountEntity.getSecurityEnvelope()))
        .deviceKey(JwkMapper.toPublicKeyDto(accountEntity.getDeviceKey()))
        .build();
  }

  public AccountDto toAccountDto(AccountEntity accountEntity) {
    return AccountDtoBuilder.builder()
        .id(accountEntity.getId())
        .publicKey(JwkMapper.toPublicKeyDto(accountEntity.getDeviceKey()))
        .build();
  }
}
