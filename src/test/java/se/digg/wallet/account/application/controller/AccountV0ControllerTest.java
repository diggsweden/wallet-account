// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.digg.wallet.account.TestUtils.publicKeyDtoBuilderWithDefaults;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.api.v0.model.AccountRequest;
import se.digg.wallet.account.api.v0.model.AccountResponse;
import se.digg.wallet.account.api.v0.model.KeyRequest;
import se.digg.wallet.account.api.v0.model.KeysResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeRequest;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeType;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopesResponse;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.domain.service.JwkValidationService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AccountV0Controller.class)
public class AccountV0ControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AccountService accountService;

  @MockitoBean
  private JwkValidationService jwkValidationService;

  @Autowired
  private ObjectMapper objectMapper;

  private final static String PERSONAL_IDENTITY_NUMBER = "2010101010";
  private final static String EMAIL = "test.testsson@test.xx";
  private final static String EMPTY = "";


  private final AccountDto resultDto = AccountDtoBuilder.builder()
    .emailAdress(EMAIL)
    .id(UUID.randomUUID())
    .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
    .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults(randomId()).build())
    .build();

  @Test
  void createAccount_nullPersonalIdentityNumber_returnsBadRequest() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email(EMAIL)
      .personalIdentityNumber(null)
      .deviceKey(keyRequestWithDefaults(randomId()))
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccount_emptyPersonalIdentityNumber_returnsBadRequest() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email(EMAIL)
      .personalIdentityNumber(EMPTY)
      .deviceKey(keyRequestWithDefaults(randomId()))
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccount_nullEmail_returnsBadRequest() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email(null)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .deviceKey(keyRequestWithDefaults(randomId()))
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccount_emptyEmail_returnsBadRequest() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email(EMPTY)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .deviceKey(keyRequestWithDefaults(randomId()))
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccount_badEmailFormat_returnsBadRequest() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email("test.testsson.se")
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .deviceKey(keyRequestWithDefaults(randomId()))
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccount_nullDeviceKey_returnsBadRequest() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email(EMAIL)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .deviceKey(null)
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccount_emptyDeviceKid_returnsBadRequest() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email(EMAIL)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .deviceKey(keyRequestWithDefaults(null))
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isBadRequest());
  }

  // TODO
  @Test
  void createAccount_acceptableRequest_returnsCreated() throws Exception {
    var accountRequest = AccountRequest.builder()
      .email(EMAIL)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .deviceKey(keyRequestWithDefaults(randomId()))
      .build();

    mockMvc
      .perform(post("/v0/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(accountRequest)))
      .andExpect(status().isCreated())
      .andReturn();
  }

  @Test
  void getAccount_nonUuidFormattedAccountId_returnsBadRequest() throws Exception {

    var nonUuidFormattedAccountId = "1234567890";

    mockMvc
      .perform(get("/v0/accounts/{0}", nonUuidFormattedAccountId))
      .andExpect(status().isBadRequest());
  }

  // TODO
  @Test
  void getAccount_nonExistingAccountId_returnsNotFound() throws Exception {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(get("/v0/accounts/{0}", UUID.randomUUID()))
      .andExpect(status().isNotFound());
  }

  // TODO
  @Test
  void getAccount_existingAccountId_returnsExpectedAccount() throws Exception {

    var expectedAccountId = UUID.randomUUID();
    var existingAccount = AccountDtoBuilder.builder()
      .id(expectedAccountId)
      .emailAdress(EMAIL)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
      .build();

    when(jwkValidationService.validateJwk(any())).thenReturn(true);
    when(accountService.getAccountById(expectedAccountId)).thenReturn(Optional.of(existingAccount));

    var result = mockMvc
      .perform(get("/v0/accounts/{0}", UUID.randomUUID()))
      .andExpect(status().isOk())
      .andReturn();
    var accountResponse = objectMapper
      .readValue(result.getResponse().getContentAsString(), AccountResponse.class);
    var actualAccountId = accountResponse.getId();

    assertThat(actualAccountId).isEqualTo(expectedAccountId);
  }

  @Test
  void addWalletKey_nullKid_returnsBadRequest() throws Exception {

    var keyRequest = keyRequestWithDefaults(randomId())
      .kid(null);

    mockMvc
      .perform(post("/v0/accounts/{0}/wallet-keys", UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(keyRequest)))
      .andExpect(status().isBadRequest());
  }

  // TODO
  @Test
  void addWalletKey_acceptableRequest_returnsCreated() throws Exception {

    var keyRequest = keyRequestWithDefaults(randomId());

    mockMvc
      .perform(post("/v0/accounts/{0}/wallet-keys", UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(keyRequest)))
      .andExpect(status().isCreated());
  }

  // TODO
  @Test
  void getWalletKey_nonExistingUserAccount_returnsNotFound() throws Exception {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    var result = mockMvc
      .perform(get("/v0/accounts/{0}/wallet-keys", UUID.randomUUID()))
      .andExpect(status().isNotFound());
  }

  @Test
  void getWalletKey_nonExistingKey_returnsEmptyResult() throws Exception {

    var result = mockMvc
      .perform(get("/v0/accounts/{0}/wallet-keys", UUID.randomUUID()))
      .andExpect(status().isOk())
      .andReturn();
    var walletKeysResponse = objectMapper
      .readValue(result.getResponse().getContentAsString(), KeysResponse.class);
    var actualItems = walletKeysResponse.getItems();

    assertThat(actualItems).isEmpty();
  }

  // TODO
  @Test
  void getWalletKey_existingUserWithWalletKey_returnsOneExpectedKey() throws Exception {

    var expectedKid = randomId();
    var existingAccount = AccountDtoBuilder.builder()
      .id(UUID.randomUUID())
      .emailAdress(EMAIL)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .publicKey(publicKeyDtoBuilderWithDefaults(expectedKid).build())
      .build();

    when(accountService.getAccountById(any())).thenReturn(Optional.of(existingAccount));

    var result = mockMvc
      .perform(get("/v0/accounts/{0}/wallet-keys", UUID.randomUUID()))
      .andExpect(status().isOk())
      .andReturn();
    var walletKeysResponse = objectMapper
      .readValue(result.getResponse().getContentAsString(), KeysResponse.class);
    var actualItems = walletKeysResponse.getItems();

    assertThat(actualItems).hasSize(1);
    var actualKid = actualItems.getFirst().getKid();
    assertThat(actualKid).isEqualTo(expectedKid);
  }

  @Test
  void addSecurityEnvelope_nullContent_returnsBadRequest() throws Exception {

    var securityEnvelopeRequest = SecurityEnvelopeRequest.builder()
      .type(SecurityEnvelopeType.SIGN)
      .content(null)
      .build();

    mockMvc
      .perform(post("/v0/accounts/{0}/security-envelopes", UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(securityEnvelopeRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void addSecurityEnvelope_nullType_returnsBadRequest() throws Exception {

    var securityEnvelopeRequest = SecurityEnvelopeRequest.builder()
      .type(null)
      .content(randomId())
      .build();

    mockMvc
      .perform(post("/v0/accounts/{0}/security-envelopes", UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(securityEnvelopeRequest)))
      .andExpect(status().isBadRequest());
  }

  // TODO
  @Test
  void addSecurityEnvelope_acceptableRequest_returnsCreated() throws Exception {

    var securityEnvelopeRequest = SecurityEnvelopeRequest.builder()
      .type(SecurityEnvelopeType.SIGN)
      .content(UUID.randomUUID().toString())
      .build();

    mockMvc
      .perform(post("/v0/accounts/{0}/security-envelopes", UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(securityEnvelopeRequest)))
      .andExpect(status().isCreated());
  }

  @Test
  void getSecurityEnvelope_nonExistingUserAccount_returnsNotFound() throws Exception {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(get("/v0/accounts/{0}/security-envelopes", UUID.randomUUID()))
      .andExpect(status().isNotFound());
  }

  @Test
  void getSecurityEnvelope_nonExistingEnvelope_returnsEmptyResult() throws Exception {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    var result = mockMvc
      .perform(get("/v0/accounts/{0}/security-envelopes", UUID.randomUUID()))
      .andExpect(status().isOk())
      .andReturn();
    var securityEnvelopesResponse = objectMapper
      .readValue(result.getResponse().getContentAsString(), SecurityEnvelopesResponse.class);
    var actualItems = securityEnvelopesResponse.getItems();

    assertThat(actualItems).isEmpty();
  }

  // TODO
  @Test
  void getSecurityEnvelope_existingUserWithEnvelope_returnsOneExpectedEnvelope() throws Exception {

    var expectedContent = randomId();
    var existingAccount = AccountDtoBuilder.builder()
      .id(UUID.randomUUID())
      .emailAdress(EMAIL)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
      // TODO add security envelope with expected content
      .build();

    when(accountService.getAccountById(any())).thenReturn(Optional.of(existingAccount));

    var result = mockMvc
      .perform(get("/v0/accounts/{0}/security-envelopes", UUID.randomUUID()))
      .andExpect(status().isOk())
      .andReturn();
    var securityEnvelopesResponse = objectMapper
      .readValue(result.getResponse().getContentAsString(), SecurityEnvelopesResponse.class);
    var actualItems = securityEnvelopesResponse.getItems();

    assertThat(actualItems).hasSize(1);
    var actualSecurityEnvelope = actualItems.getFirst();
    var actualContent = actualSecurityEnvelope.getContent();
    assertThat(actualContent).isEqualTo(expectedContent);
  }

  // TODO
  @Test
  void getSecurityEnvelope_existingUserWithEnvelopeFilterByType_returnsOneExpectedEnvelope() throws Exception {

    var expectedType = SecurityEnvelopeType.SIGN;
    var existingAccount = AccountDtoBuilder.builder()
      .id(UUID.randomUUID())
      .emailAdress(EMAIL)
      .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
      .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
      // TODO add security envelope with expected type
      .build();

    when(accountService.getAccountById(any())).thenReturn(Optional.of(existingAccount));

    var result = mockMvc
      .perform(get("/v0/accounts/{0}/security-envelopes?type={1}",
        UUID.randomUUID(), expectedType))
      .andExpect(status().isOk())
      .andReturn();
    var securityEnvelopesResponse = objectMapper
      .readValue(result.getResponse().getContentAsString(), SecurityEnvelopesResponse.class);
    var actualItems = securityEnvelopesResponse.getItems();

    assertThat(actualItems).hasSize(1);
    var actualSecurityEnvelope = actualItems.getFirst();
    var actualType = actualSecurityEnvelope.getType();
    assertThat(actualType).isEqualTo(expectedType);
  }

  private static String randomId() {
    return UUID.randomUUID().toString();
  }

  private static KeyRequest keyRequestWithDefaults(String kid) {
    return KeyRequest.builder()
      .kid(kid)
      .kty("EC")
      .crv("P-256")
      .x("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4")
      .y("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM")
      .alg("alg")
      .use("enc")
      .build();
  }
}
