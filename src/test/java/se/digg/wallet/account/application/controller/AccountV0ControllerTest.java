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

import java.util.Collections;
import java.util.List;
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
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
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
  void assertThatCreateAccount_withNullPersonalIdentityNumber_returnsBadRequest() throws Exception {
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
  void assertThatCreateAccount_withEmptyPersonalIdentityNumber_returnsBadRequest()
      throws Exception {
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
  void assertThatCreateAccount_withNullEmail_returnsBadRequest() throws Exception {
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
  void assertThatCreateAccount_withEmptyEmail_returnsBadRequest() throws Exception {
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
  void assertThatCreateAccount_withBadEmailFormat_returnsBadRequest() throws Exception {
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
  void assertThatCreateAccount_withNullDeviceKey_returnsBadRequest() throws Exception {
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
  void assertThatCreateAccount_withEmptyDeviceKid_returnsBadRequest() throws Exception {
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

  @Test
  void assertThatCreateAccount_withAcceptableRequest_returnsCreatedAccountWithId()
      throws Exception {

    var accountRequest = AccountRequest.builder()
        .email(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .deviceKey(keyRequestWithDefaults(randomId()))
        .build();
    var createdAccount = AccountDtoBuilder.builder()
        .id(UUID.randomUUID())
        .emailAdress(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();

    when(jwkValidationService.validateJwk(any())).thenReturn(true);
    when(accountService.createAccount(any())).thenReturn(createdAccount);

    var result = mockMvc
        .perform(post("/v0/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(accountRequest)))
        .andExpect(status().isCreated())
        .andReturn();
    var accountResponse = objectMapper
        .readValue(result.getResponse().getContentAsString(), AccountResponse.class);
    var actualAccountId = accountResponse.getId();

    assertThat(actualAccountId).isNotNull();
  }

  @Test
  void assertThatGetAccount_usingNonUuidFormattedAccountId_returnsBadRequest() throws Exception {

    var nonUuidFormattedAccountId = "1234567890";

    mockMvc
        .perform(get("/v0/accounts/{0}", nonUuidFormattedAccountId))
        .andExpect(status().isBadRequest());
  }

  @Test
  void assertThatGetAccount_usingNonExistingAccountId_returnsNotFound() throws Exception {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/v0/accounts/{0}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void assertThatGetAccount_usingExistingAccountId_returnsExpectedAccount() throws Exception {

    var expectedAccountId = UUID.randomUUID();
    var existingAccount = AccountDtoBuilder.builder()
        .id(expectedAccountId)
        .emailAdress(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();

    when(accountService.getAccountById(any(UUID.class))).thenReturn(Optional.of(existingAccount));

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
  void assertThatAddWalletKey_withNulllKid_returnsBadRequest() throws Exception {

    var keyRequest = keyRequestWithDefaults(randomId())
        .kid(null);

    mockMvc
        .perform(post("/v0/accounts/{0}/wallet-keys", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(keyRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void assertThatAddWalletKey_withAcceptableRequest_returnsCreated() throws Exception {

    var existingAccount = AccountDtoBuilder.builder()
        .id(UUID.randomUUID())
        .emailAdress(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();
    var createdWalletKey = publicKeyDtoBuilderWithDefaults(randomId()).build();

    when(accountService.getAccountById(any(UUID.class))).thenReturn(Optional.of(existingAccount));
    when(jwkValidationService.validateJwk(any())).thenReturn(true);
    when(accountService.createWalletKey(any(), any())).thenReturn(createdWalletKey);

    var keyRequest = keyRequestWithDefaults(randomId());

    mockMvc
        .perform(post("/v0/accounts/{0}/wallet-keys", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(keyRequest)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  @Test
  void assertThatGetWalletKey_usingNonExistingUserAccount_returnsNotFound() throws Exception {

    when(accountService.getAccountById(any(UUID.class))).thenReturn(Optional.empty());

    var result = mockMvc
        .perform(get("/v0/accounts/{0}/wallet-keys", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void assertThatGetWalletKey_userAccountWithoutKey_returnsEmptyList() throws Exception {

    var existingAccount = AccountDtoBuilder.builder()
        .id(UUID.randomUUID())
        .emailAdress(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();

    when(accountService.getAccountById(any(UUID.class))).thenReturn(Optional.of(existingAccount));
    when(accountService.getWalletKey(any(UUID.class))).thenReturn(Optional.empty());

    var result = mockMvc
        .perform(get("/v0/accounts/{0}/wallet-keys", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andReturn();
    var walletKeysResponse = objectMapper
        .readValue(result.getResponse().getContentAsString(), KeysResponse.class);
    var actualItems = walletKeysResponse.getItems();

    assertThat(actualItems).isEmpty();
  }

  @Test
  void assertThatGetWalletKey_existingUserWithWalletKey_returnsOneExpectedKey() throws Exception {

    var existingAccount = AccountDtoBuilder.builder()
        .id(UUID.randomUUID())
        .emailAdress(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();
    var expectedKid = randomId();
    var existingWalletKey = publicKeyDtoWithDefaults(expectedKid);

    when(accountService.getAccountById(any(UUID.class))).thenReturn(Optional.of(existingAccount));
    when(accountService.getWalletKey(any(UUID.class))).thenReturn(Optional.of(existingWalletKey));

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
  void assertThatAddSecurityEnvelope_withNullContent_returnsBadRequest() throws Exception {

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
  void assertThatAddSecurityEnvelope_withNullType_returnsBadRequest() throws Exception {

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

  @Test
  void assertThatAddSecurityEnvelope_withAcceptableRequest_returnsCreated() throws Exception {

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
  void assertThatGetSecurityEnvelope_usingNonExistingUserAccount_returnsNotFound()
      throws Exception {

    when(accountService.getAccountById(any(UUID.class))).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/v0/accounts/{0}/security-envelopes", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void assertThatGetSecurityEnvelope_existingAccountWithoutEnvelope_returnsEmptyList()
      throws Exception {

    var existingAccount = AccountDtoBuilder.builder()
        .id(UUID.randomUUID())
        .emailAdress(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();

    when(accountService.getAccountById(any())).thenReturn(Optional.of(existingAccount));
    when(accountService.getSecurityEnvelope(any())).thenReturn(Optional.empty());

    var result = mockMvc
        .perform(get("/v0/accounts/{0}/security-envelopes", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andReturn();
    var securityEnvelopesResponse = objectMapper
        .readValue(result.getResponse().getContentAsString(), SecurityEnvelopesResponse.class);
    var actualItems = securityEnvelopesResponse.getItems();

    assertThat(actualItems).isEmpty();
  }

  @Test
  void assertThatGetSecurityEnvelope_existingAccountWithEnvelope_returnsOneExpectedEnvelope()
      throws Exception {

    var expectedContent = randomId();
    var existingAccount = AccountDtoBuilder.builder()
        .id(UUID.randomUUID())
        .emailAdress(EMAIL)
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .publicKey(publicKeyDtoBuilderWithDefaults(randomId()).build())
        .build();

    when(accountService.getAccountById(any())).thenReturn(Optional.of(existingAccount));
    when(accountService.getSecurityEnvelope(any())).thenReturn(Optional.of(expectedContent));

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

  private static String randomId() {
    return UUID.randomUUID().toString();
  }

  private static PublicKeyDto publicKeyDtoWithDefaults(String kid) {
    return PublicKeyDtoBuilder.builder()
        .kty("EC")
        .crv("P-256")
        .x("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4")
        .y("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM")
        .alg("alg")
        .use("enc")
        .kid(kid)
        .build();
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
