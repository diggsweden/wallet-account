// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.api.v0.model.KeyRequest;
import se.digg.wallet.account.api.v0.model.KeyResponse;
import se.digg.wallet.account.api.v0.model.KeysResponse;
import se.digg.wallet.account.api.v0.model.ProblemParameterResponse;
import se.digg.wallet.account.api.v0.model.ProblemResponse;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.liquibase.enabled=false"
    })
public class AccountApiWalletKeyComponentTest {

  private static final String VALIDATION_FAILURE = "/problem-details/field-validation-failure";
  private static final UUID ACCOUNT_ID = UUID.fromString("61128b3c-ef55-4410-8dff-d8e8bf0cb9a7");
  private static final String KEY_ID = "26862913-ecd0-4d4d-a3d0-9271665d577e";
  private static final String TRANSACTION_ID = "a7240655-a568-41c8-8059-7b18859d5d88";

  private RestTestClient client;

  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @MockitoBean
  private AccountService accountService;

  @BeforeEach
  void setUp(WebApplicationContext context) {
    client = RestTestClient.bindToApplicationContext(context).build();
    MDC.put("transactionId", TRANSACTION_ID);
  }

  @AfterEach
  void cleanUp() {
    MDC.clear();
  }

  @Test
  void addingWalletKeyToNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.post()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .body(defaultKeyRequest().build())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "kid",
      "kty",
      "crv",
      "x",
      "y"
  })
  void addWalletKeyWithoutRequiredPropertyReturnsPropertyProblem(String invalidProperty) {

    var problemResponse = client.post()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .body(KeyRequest.builder().build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST, VALIDATION_FAILURE,
        invalidProperty);
  }

  @Test
  void addingInvalidWalletKeyReturnsBadRequest() {

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    var invalidKey = KeyRequest.builder()
        .kid("1")
        .kty("2")
        .crv("3")
        .x("4")
        .y("5")
        .build();

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));

    client.post()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .body(invalidKey)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void addsWalletKeyToAccount() {

    final KeyRequest walletKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto walletKeyDto = toPublicKeyDto(walletKeyRequest);

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));
    when(accountService.createWalletKey(any(), any())).thenReturn(walletKeyDto);

    var keyResponse = client.post()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .body(walletKeyRequest)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(KeyResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(keyResponse).isNotNull().isEqualTo(toKeyResponse(walletKeyRequest));
  }

  @Test
  void fetchingWalletKeysFromNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void fetchingNonExistingWalletKeyReturnsEmptyList() {

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));
    when(accountService.getWalletKey(any())).thenReturn(Optional.empty());

    var keysResponse = client.get()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(KeysResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(keysResponse).isNotNull();
    assertThat(keysResponse.getItems()).isEmpty();
  }

  @Test
  void servesWalletKeys() {

    final KeyRequest walletKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto walletKeyDto = toPublicKeyDto(walletKeyRequest);

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));
    when(accountService.getWalletKey(any())).thenReturn(Optional.of(walletKeyDto));

    var keysResponse = client.get()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(KeysResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(keysResponse).isNotNull();
    assertThat(keysResponse.getItems()).isNotEmpty().hasSize(1);
    assertThat(keysResponse.getItems().getFirst()).isEqualTo(toKeyResponse(walletKeyRequest));
  }

  @Test
  void servesWalletKeyById() {

    final KeyRequest walletKeyRequest = defaultKeyRequest()
        .kid(KEY_ID)
        .build();
    final PublicKeyDto walletKeyDto = toPublicKeyDto(walletKeyRequest);

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));
    when(accountService.getWalletKey(any())).thenReturn(Optional.of(walletKeyDto));

    var keysResponse = client.get()
        .uri("/v0/accounts/{0}/wallet-keys?kid={1}", ACCOUNT_ID, KEY_ID)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(KeysResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(keysResponse).isNotNull();
    assertThat(keysResponse.getItems()).isNotEmpty().hasSize(1);
    assertThat(keysResponse.getItems().getFirst()).isEqualTo(toKeyResponse(walletKeyRequest));
  }

  private static KeyRequest.Builder defaultKeyRequest() {
    return KeyRequest.builder()
        .kid(KEY_ID)
        .kty("EC")
        .crv("P-256")
        .x("1fH0eqXgMMwCIafNaDc1axdCjLlw7zpTLvLWjpPvhEc")
        .y("5qOejJs7BK-jLingaUTEhBrzP_YPyHfptS5yWE98I40");
  }

  private static PublicKeyDto toPublicKeyDto(KeyRequest keyRequest) {
    return new PublicKeyDto(
        keyRequest.getKty(),
        keyRequest.getKid(),
        null,
        null,
        keyRequest.getCrv(),
        keyRequest.getX(),
        keyRequest.getY());
  }

  private static KeyResponse toKeyResponse(KeyRequest keyRequest) {
    return KeyResponse.builder()
        .kty(keyRequest.getKty())
        .kid(keyRequest.getKid())
        .crv(keyRequest.getCrv())
        .x(keyRequest.getX())
        .y(keyRequest.getY())
        .build();
  }

  private static void assertProblemDetails(ProblemResponse problemResponse,
      HttpStatus expectedHttpStatus,
      @Nullable String expectedType,
      @Nullable String expectedInvalidParameterProperty) {

    assertThat(problemResponse).isNotNull();
    assertThat(problemResponse.getStatus()).isEqualTo(expectedHttpStatus.value());
    assertThat(problemResponse.getTitle()).isNotEmpty();
    assertThat(problemResponse.getDetail()).isPresent();
    assertThat(problemResponse.getInstance()).isNotEmpty();
    assertThat(problemResponse.getType()).isPresent();
    assertThat(problemResponse.getTransactionId()).isPresent().get().isEqualTo(TRANSACTION_ID);
    if (expectedType != null) {
      assertThat(problemResponse.getType()).get().isEqualTo(expectedType);
    }

    if (expectedInvalidParameterProperty != null) {
      assertThat(problemResponse.getInvalidParameters()).isNotEmpty();
      assertThat(expectedInvalidParameterProperty).isIn(problemResponse.getInvalidParameters()
          .stream()
          .map(ProblemParameterResponse::getProperty)
          .map(value -> value.orElse(null))
          .filter(Objects::nonNull)
          .toList());
    }
  }
}
