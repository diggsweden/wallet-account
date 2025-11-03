// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.infrastructure.mapper.AccountEntityMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock
  AccountRepository accountRepository;

  @Mock
  AccountEntityMapper accountEntityMapper;

  @InjectMocks
  AccountService accountService;

  @Test
  void testDuplicatedUsers() {

    AccountEntity testAccountEntity = new AccountEntity("770707-7777", "dummy", null, null);
    testAccountEntity.setId(UUID.randomUUID());
    // mocking
    when(accountRepository.findByPersonalIdentityNumber("770707-7777"))
        .thenReturn(List.of(testAccountEntity));
    when(accountRepository.save(any())).thenReturn(testAccountEntity);
    when(accountEntityMapper.toAccountDto(any()))
        .thenReturn(TestUtils.accountDtoBuilderWithDefults().build());

    CreateAccountRequestDto accountRequestDto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("dummy2@dummy.se")
        .personalIdentityNumber("770707-7777")
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("1").build())
        .build();

    assertThat(accountService.createAccount(accountRequestDto)).isNotNull();


  }

  @Test
  void testSaveUsers() {
    AccountEntity testAccountEntity = new AccountEntity("770707-7777", "dummy", null, null);
    testAccountEntity.setId(UUID.randomUUID());
    // mocking
    when(accountRepository.findByPersonalIdentityNumber("770707-7777"))
        .thenReturn(List.of()); // empty list
    when(accountRepository.save(any())).thenReturn(testAccountEntity);
    when(accountEntityMapper.toAccountDto(any()))
        .thenReturn(TestUtils.accountDtoBuilderWithDefults().build());

    CreateAccountRequestDto accountRequestDto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("dummy2@dummy.se")
        .personalIdentityNumber("770707-7777")
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("1").build())
        .build();

    assertThat(accountService.createAccount(accountRequestDto)).isNotNull();
  }

}
