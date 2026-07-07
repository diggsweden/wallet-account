// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

  private RestTestClient client;

  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @MockitoBean
  private AccountService accountService;

  @BeforeEach
  void setUp(WebApplicationContext context) {
    client = RestTestClient.bindToApplicationContext(context).build();
  }

  @Test
  void addingWalletKeyToNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.post()
        .uri("/v0/accounts/{0}/wallet-keys", UUID.randomUUID())
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
        .uri("/v0/accounts/{0}/wallet-keys", UUID.randomUUID())
        .body(KeyRequest.builder().build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(problemResponse).isNotNull();
    assertThat(problemResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problemResponse.getTitle()).isNotEmpty();
    assertThat(problemResponse.getDetail()).isNotEmpty();
    assertThat(problemResponse.getType()).isNotEmpty();
    assertThat(problemResponse.getType().get()).isEqualTo(VALIDATION_FAILURE);
    assertThat(problemResponse.getInvalidParameters()).isNotEmpty();
    assertThat(invalidProperty).isIn(problemResponse.getInvalidParameters().stream()
        .map(ProblemParameterResponse::getProperty)
        .map(value -> value.orElse(null))
        .filter(Objects::nonNull)
        .toList());
  }

  @Test
  void addingInvalidWalletKeyReturnsBadRequest() {

    final UUID accountId = UUID.randomUUID();

    var accountDto = new AccountDto(
        accountId,
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

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));

    var problemResponse = client.post()
        .uri("/v0/accounts/{0}/wallet-keys", accountId)
        .body(invalidKey)
        .exchange()
        .expectStatus();
  }

  @Test
  void addsWalletKeyToAccount() {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest walletKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto walletKeyDto = toPublicKeyDto(walletKeyRequest);

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.createWalletKey(any(), any())).thenReturn(walletKeyDto);

    var keyResponse = client.post()
        .uri("/v0/accounts/{0}/wallet-keys", accountId)
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
        .uri("/v0/accounts/{0}/wallet-keys", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void fetchingNonExistingWalletKeyReturnsEmptyList() {

    final UUID accountId = UUID.randomUUID();

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.getWalletKey(any())).thenReturn(Optional.empty());

    var keysResponse = client.get()
        .uri("/v0/accounts/{0}/wallet-keys", accountId)
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

    final UUID accountId = UUID.randomUUID();
    final KeyRequest walletKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto walletKeyDto = toPublicKeyDto(walletKeyRequest);

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.getWalletKey(any())).thenReturn(Optional.of(walletKeyDto));

    var keysResponse = client.get()
        .uri("/v0/accounts/{0}/wallet-keys", accountId)
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
  void servesWalletKeysFilterByKeyId() {

    final UUID accountId = UUID.randomUUID();
    final String keyId = UUID.randomUUID().toString();
    final KeyRequest walletKeyRequest = defaultKeyRequest()
        .kid(keyId)
        .build();
    final PublicKeyDto walletKeyDto = toPublicKeyDto(walletKeyRequest);

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.getWalletKey(any())).thenReturn(Optional.of(walletKeyDto));

    var keysResponse = client.get()
        .uri("/v0/accounts/{0}/wallet-keys?kid={1}", accountId, keyId)
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
        .kid(UUID.randomUUID().toString())
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
}
