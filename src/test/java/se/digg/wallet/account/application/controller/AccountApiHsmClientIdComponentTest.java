// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.api.v0.model.HsmClientIdRequest;
import se.digg.wallet.account.api.v0.model.HsmClientIdResponse;
import se.digg.wallet.account.api.v0.model.KeyRequest;
import se.digg.wallet.account.api.v0.model.KeyResponse;
import se.digg.wallet.account.api.v0.model.ProblemResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeRequest;
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
public class AccountApiHsmClientIdComponentTest {

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
  void addingHsmClientIdToNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.post()
        .uri("/v0/accounts/{0}/hsm-client-id", UUID.randomUUID())
        .body(HsmClientIdRequest.builder()
            .clientId(UUID.randomUUID().toString())
            .build())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void addingEmptyHsmClientIdReturnsHsmClientIdProblem(String emptyClientId) {

    var problemResponse = client.post()
        .uri("/v0/accounts/{0}/hsm-client-id", UUID.randomUUID())
        .body(HsmClientIdRequest.builder()
            .clientId(emptyClientId)
            .build())
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
    assertThat(problemResponse.getInvalidParameters().getFirst().getProperty())
        .contains("clientId");
  }

  @Test
  void addsHsmClientIdToAccount() {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest walletKeyRequest = defaultKeyRequest().build();
    final String clientId = UUID.randomUUID().toString();

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.createHsmClientId(any(), any())).thenReturn(clientId);

    var hsmClientIdResponse = client.post()
        .uri("/v0/accounts/{0}/hsm-client-id", accountId)
        .body(HsmClientIdRequest.builder()
            .clientId(clientId)
            .build())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(HsmClientIdResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(hsmClientIdResponse).isNotNull();
    assertThat(hsmClientIdResponse.getClientId()).isNotEmpty().isEqualTo(clientId);
  }

  @Test
  void fetchingHsmClientIdFromNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}/hsm-client-id", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void fetchingNonExistingHsmClientIdReturnsNotFound() {

    final UUID accountId = UUID.randomUUID();

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.getHsmClientId(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}/hsm-client-id", accountId)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void servesHsmClientId() {

    final UUID accountId = UUID.randomUUID();
    final String clientId = UUID.randomUUID().toString();

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.getHsmClientId(any())).thenReturn(Optional.of(clientId));

    var hsmClientIdResponse = client.get()
        .uri("/v0/accounts/{0}/hsm-client-id", accountId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(HsmClientIdResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(hsmClientIdResponse).isNotNull();
    assertThat(hsmClientIdResponse.getClientId()).isNotEmpty().isEqualTo(clientId);
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
