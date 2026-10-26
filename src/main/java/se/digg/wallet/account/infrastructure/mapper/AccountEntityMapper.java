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
import se.digg.wallet.account.infrastructure.model.DeviceKey;
import se.digg.wallet.account.infrastructure.model.WalletEntity;

@Service
public class AccountEntityMapper {


  public DeviceKey toDeviceKey(CreateAccountRequestDto accountRequestDto) {
    return JwkMapper.toDeviceKey(accountRequestDto.publicKey());
  }

  public ExtendedAccountDto toExtendedAccountDto(WalletEntity wallet) {
    return ExtendedAccountDtoBuilder.builder()
        .id(wallet.getAccount().getId())
        .securityEnvelope(BytesMapper.bytesToString(wallet.getSecurityEnvelope()))
        .deviceKey(JwkMapper.toPublicKeyDto(wallet.getDeviceKey()))
        .build();
  }

  public AccountDto toAccountDto(WalletEntity wallet) {
    return AccountDtoBuilder.builder()
        .id(wallet.getAccount().getId())
        .publicKey(JwkMapper.toPublicKeyDto(wallet.getDeviceKey()))
        .build();
  }
}
