// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static se.digg.wallet.account.application.controller.util.ProblemDetailAssertion.assertProblemDetails;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.api.v0.model.AccountRequest;
import se.digg.wallet.account.api.v0.model.AccountResponse;
import se.digg.wallet.account.api.v0.model.EcJwkRequest;
import se.digg.wallet.account.api.v0.model.HsmClientIdRequest;
import se.digg.wallet.account.api.v0.model.ProblemResponse;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;
import se.digg.wallet.account.infrastructure.repository.WalletRepository;

/**
 * Concurrent requests can both pass the service's duplicate check. These tests let the check pass
 * and verify that the database's unique constraints are mapped to 409, not 500.
 *
 * <p>
 * No test transaction: a constraint violation aborts the surrounding PostgreSQL transaction, so
 * every test uses freshly generated keys instead of rolling back.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class UniqueConstraintConflictItTest {

  private static final String TRANSACTION_ID = "2b0d7c5e-4a8f-4f6e-9c3a-1d2e3f4a5b6c";
  private static final String ALREADY_EXISTS = "/problem-details/resource-already-exists";

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @MockitoSpyBean
  WalletRepository walletRepository;

  private RestTestClient client;

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
  void concurrentRegistrationOfTheSameKidReturnsConflict() throws JOSEException {
    var kid = UUID.randomUUID().toString();
    createAccount(deviceKey(newKey(), kid));
    duplicateCheckFindsNothing();

    var problem = createAccountExpectingConflict(deviceKey(newKey(), kid));

    assertProblemDetails(problem, HttpStatus.CONFLICT, TRANSACTION_ID, ALREADY_EXISTS, null);
  }

  @Test
  void concurrentRegistrationOfTheSameKeyUnderANewKidReturnsConflict() throws JOSEException {
    var key = newKey();
    createAccount(deviceKey(key, UUID.randomUUID().toString()));
    duplicateCheckFindsNothing();

    var problem = createAccountExpectingConflict(deviceKey(key, UUID.randomUUID().toString()));

    assertProblemDetails(problem, HttpStatus.CONFLICT, TRANSACTION_ID, ALREADY_EXISTS, null);
  }

  @Test
  void hsmClientIdInUseByAnotherWalletReturnsConflict() throws JOSEException {
    var clientId = UUID.randomUUID().toString();
    var first = createAccount(deviceKey(newKey(), UUID.randomUUID().toString()));
    var second = createAccount(deviceKey(newKey(), UUID.randomUUID().toString()));
    addHsmClientId(first.getId(), clientId).expectStatus().isCreated();

    var problem = addHsmClientId(second.getId(), clientId)
        .expectStatus().isEqualTo(HttpStatus.CONFLICT.value())
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertProblemDetails(problem, HttpStatus.CONFLICT, TRANSACTION_ID, ALREADY_EXISTS, null);
  }

  @Test
  void otherDataIntegrityViolationsAreNotMappedToConflict() throws JOSEException {
    doThrow(new DataIntegrityViolationException("some other constraint"))
        .when(walletRepository).saveAndFlush(any());

    client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .deviceKey(deviceKey(newKey(), UUID.randomUUID().toString()))
            .build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  private void duplicateCheckFindsNothing() {
    doReturn(false).when(walletRepository).existsByDeviceKeyKidOrDeviceKeyThumbprint(any(), any());
  }

  private AccountResponse createAccount(EcJwkRequest deviceKey) {
    return client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder().deviceKey(deviceKey).build())
        .exchange()
        .expectStatus().isCreated()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();
  }

  private ProblemResponse createAccountExpectingConflict(EcJwkRequest deviceKey) {
    return client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder().deviceKey(deviceKey).build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT.value())
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();
  }

  private RestTestClient.ResponseSpec addHsmClientId(UUID accountId, String clientId) {
    return client.post()
        .uri("/v0/accounts/{0}/hsm-client-id", accountId)
        .body(HsmClientIdRequest.builder().clientId(clientId).build())
        .exchange();
  }

  private static ECKey newKey() throws JOSEException {
    return new ECKeyGenerator(Curve.P_256).generate();
  }

  private static EcJwkRequest deviceKey(ECKey key, String kid) {
    return EcJwkRequest.builder()
        .kid(kid)
        .kty("EC")
        .crv("P-256")
        .x(key.getX().toString())
        .y(key.getY().toString())
        .build();
  }
}
