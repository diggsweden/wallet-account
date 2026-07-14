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
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
import se.digg.wallet.account.api.v0.model.ProblemParameterResponse;
import se.digg.wallet.account.api.v0.model.ProblemResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeRequest;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopesResponse;
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
public class AccountApiSecurityEnvelopeComponentTest {

  private static final String VALIDATION_FAILURE = "/problem-details/field-validation-failure";
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
  void addingSecurityEnvelopeToNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.post()
        .uri("/v0/accounts/{0}/security-envelopes", UUID.randomUUID())
        .body(SecurityEnvelopeRequest.builder()
            .content(UUID.randomUUID().toString())
            .build())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void addingEmptySecurityEnvelopeReturnsContentProblem(String emptyContent) {

    var problemResponse = client.post()
        .uri("/v0/accounts/{0}/security-envelopes", UUID.randomUUID())
        .body(SecurityEnvelopeRequest.builder()
            .content(emptyContent)
            .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST, VALIDATION_FAILURE, "content");
  }

  @Test
  void addsSecurityEnvelopeToAccount() {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest walletKeyRequest = defaultKeyRequest().build();
    final String content = UUID.randomUUID().toString();

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.createSecurityEnvelope(any(), any())).thenReturn(content);

    var securityEnvelopeResponse = client.post()
        .uri("/v0/accounts/{0}/security-envelopes", accountId)
        .body(SecurityEnvelopeRequest.builder()
            .content(content)
            .build())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(SecurityEnvelopeResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(securityEnvelopeResponse).isNotNull();
    assertThat(securityEnvelopeResponse.getContent()).isNotEmpty().isEqualTo(content);
  }

  @Test
  void fetchingSecurityEnvelopeFromNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}/security-envelopes", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void fetchingNonExistingSecurityEnvelopesShouldReturnOkEmpty() {

    final UUID accountId = UUID.randomUUID();

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.getSecurityEnvelope(any())).thenReturn(Optional.empty());

    var securityEnvelopesResponse = client.get()
        .uri("/v0/accounts/{0}/security-envelopes", accountId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(SecurityEnvelopesResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(securityEnvelopesResponse).isNotNull();
    assertThat(securityEnvelopesResponse.getItems()).isEmpty();
  }

  @Test
  void servesSecurityEnvelopes() {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest walletKeyRequest = defaultKeyRequest().build();
    final String content = UUID.randomUUID().toString();

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));
    when(accountService.getSecurityEnvelope(any())).thenReturn(Optional.of(content));

    var securityEnvelopesResponse = client.get()
        .uri("/v0/accounts/{0}/security-envelopes", accountId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(SecurityEnvelopesResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(securityEnvelopesResponse).isNotNull();
    assertThat(securityEnvelopesResponse.getItems()).isNotEmpty().hasSize(1);
    assertThat(securityEnvelopesResponse.getItems().getFirst().getContent()).isEqualTo(content);
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
