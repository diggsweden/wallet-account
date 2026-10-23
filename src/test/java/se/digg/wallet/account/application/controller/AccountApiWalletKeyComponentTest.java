// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import java.util.UUID;
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
import se.digg.wallet.account.api.v0.model.EcJwkRequest;
import se.digg.wallet.account.api.v0.model.ProblemResponse;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;

import static org.mockito.Mockito.verifyNoInteractions;
import static se.digg.wallet.account.application.controller.util.ProblemDetailAssertion.assertProblemDetails;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
  void addingWalletKeyReturnsGone() {

    client.post()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .body(defaultKeyRequest().build())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.GONE);

    verifyNoInteractions(accountService);
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
        .body(EcJwkRequest.builder().build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST, TRANSACTION_ID,
        VALIDATION_FAILURE, invalidProperty);
  }

  @Test
  void fetchingWalletKeysReturnsGone() {

    client.get()
        .uri("/v0/accounts/{0}/wallet-keys", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.GONE);

    verifyNoInteractions(accountService);
  }

  private static EcJwkRequest.Builder defaultKeyRequest() {
    return EcJwkRequest.builder()
        .kid(KEY_ID)
        .kty("EC")
        .crv("P-256")
        .x("1fH0eqXgMMwCIafNaDc1axdCjLlw7zpTLvLWjpPvhEc")
        .y("5qOejJs7BK-jLingaUTEhBrzP_YPyHfptS5yWE98I40");
  }
}
