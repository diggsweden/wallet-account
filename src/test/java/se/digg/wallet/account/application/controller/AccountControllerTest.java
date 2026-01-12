// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.domain.service.JwkValidationService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  AccountService accountService;

  @MockitoBean
  JwkValidationService jwkValidationService;

  @Autowired
  private ObjectMapper objectMapper;

  private UUID defaulUuid = UUID.randomUUID();

  private AccountDto resulDto = AccountDtoBuilder.builder()
      .emailAdress("none@your.bussiness.se")
      .id(defaulUuid)
      .personalIdentityNumber("770101-1234")
      .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("91").build())
      .build();


  @Test
  void test_happy_path_save() throws Exception {
    CreateAccountRequestDto dto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.bussiness.se")
        .personalIdentityNumber("770101-1234")
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("77").build())
        .build();

    when(jwkValidationService.validateJwk(any())).thenReturn(true);

    when(accountService.createAccount(any())).thenReturn(resulDto);
    mockMvc
        .perform(post("/account")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated());
  }

  @Test
  void test_invalid_jwk() throws Exception {
    CreateAccountRequestDto dto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.bussiness.se")
        .personalIdentityNumber("770101-1234")
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("77").build())
        .build();

    when(jwkValidationService.validateJwk(any())).thenReturn(false);

    when(accountService.createAccount(any())).thenReturn(resulDto);
    mockMvc
        .perform(post("/account")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_get_ok() throws Exception {
    when(accountService.getAccountById(defaulUuid))
        .thenReturn(Optional.of(resulDto));
    mockMvc
        .perform(
            get("/account/" + defaulUuid))
        .andExpect(status().isOk())
        .andExpect(content()
            .string(containsString(defaulUuid.toString())));
  }
}
