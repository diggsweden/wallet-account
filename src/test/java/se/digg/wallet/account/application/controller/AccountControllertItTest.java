// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.api.v0.model.ProblemResponse;
import se.digg.wallet.account.api.v0.model.AccountResponse;
import se.digg.wallet.account.api.v0.model.AccountRequest;
import se.digg.wallet.account.api.v0.model.KeyRequest;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional // Rollback happens automatically after each test
@AutoConfigureRestTestClient
class AccountControllertItTest {

  private static final String PERSONAL_IDENTITY_NUMBER = "2010101010";
  private static final String EMAIL = "test.testsson@test.test";
  private static final String PHONE_NUMBER = "0700000000";
  private static final String TRANSACTION_ID = "a7240655-a568-41c8-8059-7b18859d5d88";

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  private RestTestClient client;

  @Autowired
  AccountRepository repository;

  @BeforeEach
  void setUp(WebApplicationContext context) { // Inject the configuration
    client = RestTestClient.bindToApplicationContext(context).build();
    MDC.put("transactionId", TRANSACTION_ID);
  }

  @AfterEach
  void cleanUp() {
    MDC.clear();
  }

  @Test
  void getAccount() {

    AccountEntity accountEntity = new AccountEntity(
        PERSONAL_IDENTITY_NUMBER,
        EMAIL,
        PHONE_NUMBER,
        null,
        TestUtils.generateJwkEntity(null),
        TestUtils.generateJwkEntity("1"));
    var accountId = repository.save(accountEntity).getId();

    var accountResponse =
        client.get()
            .uri("/v0/accounts/{0}", accountId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(accountResponse).isNotNull();
    assertThat(accountResponse.getPersonalIdentityNumber()).isPresent().get()
        .isEqualTo(PERSONAL_IDENTITY_NUMBER);
    assertThat(accountResponse.getEmail()).isPresent().get().isEqualTo(EMAIL);
    assertThat(accountResponse.getPhoneNumber()).isPresent().get().isEqualTo(PHONE_NUMBER);
    assertThat(accountResponse.getDeviceKey()).isNotNull();
    assertThat(accountResponse.getDeviceKey().getKid()).isNotNull()
        .isEqualTo(accountEntity.getDeviceKey().getKid());
  }

  @Test
  void saveAccount() {

    AccountRequest requestDto =
        AccountRequest.builder()
            .email(EMAIL)
            .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
            .phoneNumber(PHONE_NUMBER)
            .deviceKey(deviceKeyWithDefaults("2"))
            .build();

    var accountResponse = client.post()
        .uri("/v0/accounts")
        .body(requestDto)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(accountResponse).isNotNull();
    assertThat(accountResponse.getId()).isNotNull();
  }

  @Test
  void testSaveDuplicateAccounts() {

    var keyId = "3";
    var accountRequest = AccountRequest.builder()
        .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
        .email(EMAIL)
        .phoneNumber(PHONE_NUMBER)
        .deviceKey(deviceKeyWithDefaults(keyId))
        .build();
    var duplicateAccountRequest = AccountRequest.builder()
        .deviceKey(deviceKeyWithDefaults(keyId))
        .build();

    var accountResponse =
        client.post()
            .uri("/v0/accounts")
            .body(accountRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(accountResponse).isNotNull();
    var accountId = accountResponse.getId();
    assertThat(accountId).isNotNull();

    // Duplicate account
    var problemResponse =
        client.post()
            .uri("/v0/accounts")
            .body(duplicateAccountRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT.value())
            .expectBody(ProblemResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(problemResponse).isNotNull();
    assertThat(problemResponse.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problemResponse.getTitle()).isNotEmpty();

    // The original account is untouched and still retrievable after the rejected duplicate.
    var fetchedAccountResponse =
        client.get()
            .uri("/v0/accounts/{0}", accountId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(fetchedAccountResponse).isNotNull();
    assertThat(fetchedAccountResponse.getPersonalIdentityNumber()).isPresent().get()
        .isEqualTo(PERSONAL_IDENTITY_NUMBER);
    assertThat(fetchedAccountResponse.getEmail()).isPresent().get().isEqualTo(EMAIL);
    assertThat(fetchedAccountResponse.getPhoneNumber()).isPresent().get().isEqualTo(PHONE_NUMBER);
    assertThat(fetchedAccountResponse.getDeviceKey()).isNotNull();
    assertThat(fetchedAccountResponse.getDeviceKey().getKid()).isNotNull().isEqualTo(keyId);
  }

  private static KeyRequest deviceKeyWithDefaults(String kid) {
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
