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
import se.digg.wallet.account.api.v0.model.HsmClientIdRequest;
import se.digg.wallet.account.api.v0.model.HsmClientIdResponse;
import se.digg.wallet.account.api.v0.model.KeyRequest;
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
public class AccountApiHsmClientIdComponentTest {

  private static final String VALIDATION_FAILURE = "/problem-details/field-validation-failure";
  private static final String HSM_CLIENT_ID = "05839bd0-e05a-41eb-9e8f-46a32f540442";
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
  void addingHsmClientIdToNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.post()
        .uri("/v0/accounts/{0}/hsm-client-id", ACCOUNT_ID)
        .body(HsmClientIdRequest.builder()
            .clientId(HSM_CLIENT_ID)
            .build())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void addingEmptyHsmClientIdReturnsHsmClientIdProblem(String emptyClientId) {

    var problemResponse = client.post()
        .uri("/v0/accounts/{0}/hsm-client-id", ACCOUNT_ID)
        .body(HsmClientIdRequest.builder()
            .clientId(emptyClientId)
            .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST, VALIDATION_FAILURE, "clientId");
  }

  @Test
  void addsHsmClientIdToAccount() {

    final KeyRequest walletKeyRequest = defaultKeyRequest().build();

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));
    when(accountService.createHsmClientId(any(), any())).thenReturn(HSM_CLIENT_ID);

    var hsmClientIdResponse = client.post()
        .uri("/v0/accounts/{0}/hsm-client-id", ACCOUNT_ID)
        .body(HsmClientIdRequest.builder()
            .clientId(HSM_CLIENT_ID)
            .build())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(HsmClientIdResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(hsmClientIdResponse).isNotNull();
    assertThat(hsmClientIdResponse.getClientId()).isNotEmpty().isEqualTo(HSM_CLIENT_ID);
  }

  @Test
  void fetchingHsmClientIdFromNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}/hsm-client-id", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void fetchingNonExistingHsmClientIdReturnsNotFound() {

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));
    when(accountService.getHsmClientId(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}/hsm-client-id", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void servesHsmClientId() {

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        toPublicKeyDto(defaultKeyRequest().build()));

    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));
    when(accountService.getHsmClientId(any())).thenReturn(Optional.of(HSM_CLIENT_ID));

    var hsmClientIdResponse = client.get()
        .uri("/v0/accounts/{0}/hsm-client-id", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(HsmClientIdResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(hsmClientIdResponse).isNotNull();
    assertThat(hsmClientIdResponse.getClientId()).isNotEmpty().isEqualTo(HSM_CLIENT_ID);
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
