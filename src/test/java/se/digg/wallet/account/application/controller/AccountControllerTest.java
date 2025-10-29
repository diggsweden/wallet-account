package se.digg.wallet.account.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.domain.service.AccountService;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  AccountService accountService;


  @Autowired
  private ObjectMapper objectMapper;
  private UUID defaulUuid = UUID.randomUUID();
  private AccountDto resulDto = AccountDtoBuilder.builder()
      .emailAdress("none@your.bussiness.se")
      .id(defaulUuid)
      .personalIdentityNumber("770101-1234")
      .publicKey(new PublicKeyDto("sdasdas", "id"))
      .build();

  @Test
  void happy_path_save() throws Exception {
    CreateAccountRequestDto dto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.bussiness.se")
        .personalIdentityNumber("770101-1234")
        .publicKey(new PublicKeyDto("sdasdas", "id"))
        .build();



    when(accountService.createAccount(any())).thenReturn(resulDto);
    mockMvc
        .perform(post("/account")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated());
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
