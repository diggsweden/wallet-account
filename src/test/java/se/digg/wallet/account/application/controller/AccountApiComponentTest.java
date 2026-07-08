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
import se.digg.wallet.account.api.v0.model.AccountRequest;
import se.digg.wallet.account.api.v0.model.AccountResponse;
import se.digg.wallet.account.api.v0.model.KeyRequest;
import se.digg.wallet.account.api.v0.model.KeyResponse;
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
public class AccountApiComponentTest {

  private static final String PERSONAL_IDENTITY_NUMBER = "2010101010";
  private static final String EMAIL = "test.testsson@test.test";
  private static final String PHONE_NUMBER = "0700000000";
  private static final String VALIDATION_FAILURE = "/problem-details/field-validation-failure";

  private RestTestClient client;

  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @MockitoBean
  private AccountService accountService;

  @BeforeEach
  void setUp(WebApplicationContext context) { // Inject the configuration
    client = RestTestClient.bindToApplicationContext(context).build();
  }

  @Test
  void createAccountWithoutDeviceKeyReturnsDeviceKeyProblem() {

    var problemResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .deviceKey(null)
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
        .contains("deviceKey");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "deviceKey.kid",
      "deviceKey.kty",
      "deviceKey.crv",
      "deviceKey.x",
      "deviceKey.y"
  })
  void informsClientOfDeviceKeyParameterProblem(String invalidProperty) {

    var problemResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .deviceKey(KeyRequest.builder().build())
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
    assertThat(invalidProperty).isIn(problemResponse.getInvalidParameters().stream()
        .map(ProblemParameterResponse::getProperty)
        .map(value -> value.orElse(null))
        .filter(Objects::nonNull)
        .toList());
  }

  @Test
  void createAccountWithInvalidWalletKeyReturnsBadRequest() {

    var invalidKey = KeyRequest.builder()
        .kid("1")
        .kty("2")
        .crv("3")
        .x("4")
        .y("5")
        .build();

    var problemResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .deviceKey(invalidKey)
            .build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      " ",
      "test@domain",
      "domain.xx",
      "test.testsson#domain.xx",
      "test.testsson@domain",
      "test.testsson.domain.se"
  })
  void createAccountWithBadEmailFormatReturnsEmailProblem(String badFormattedEmailAddress) {

    var problemResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .email(badFormattedEmailAddress)
            .deviceKey(defaultKeyRequest().build())
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
        .contains("email");
  }

  @Test
  void createAccountFailsWithUnexpectedServerErrorReturnsGenericServerProblem() {

    final var message = "The cause error message";
    var testException = new UnexpectedException(message);
    when(accountService.createAccount(any())).thenThrow(testException);

    var problemResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .deviceKey(defaultKeyRequest().build())
            .build())
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(problemResponse).isNotNull();
    assertThat(problemResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problemResponse.getTitle()).isNotEmpty();
    assertThat(problemResponse.getDetail()).isNotEmpty().get().isEqualTo(message);
    assertThat(problemResponse.getType()).isNotEmpty();
  }

  @Test
  void createAccountWithoutOptionalsReturnsSavedValues() {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        deviceKeyDto);
    when(accountService.createAccount(any())).thenReturn(accountDto);

    var accountResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .deviceKey(deviceKeyRequest)
            .build())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(accountResponse).isNotNull();
    assertThat(accountResponse.getId()).isNotNull();
    assertThat(accountResponse.getId()).isEqualTo(accountId);
    assertThat(accountResponse.getDeviceKey()).isEqualTo(toKeyResponse(deviceKeyRequest));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "a@a.se",
      "test@domain.com",
      "test.testsson@domain.xx",
      "test@domain.sub.eu",
      "a-very-long-firstname.and.another-long-lasting-lastname@sub-department.the-domain.net",
      "123@sub2.sub1.domain.com",
      "First.Last@Mixed.Se",
      "ONLY.CAPITAL.LETTERS@THE.DOMAIN.COM",
      "100@100.se"
  })
  void acceptsCreateAccountRequestsWithValidEmail(String email) {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        accountId,
        Optional.empty(),
        Optional.of(email),
        Optional.empty(),
        deviceKeyDto);
    when(accountService.createAccount(any())).thenReturn(accountDto);

    var accountResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .personalIdentityNumber(null)
            .email(email)
            .phoneNumber(null)
            .deviceKey(deviceKeyRequest)
            .build())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(accountResponse).isNotNull();
    assertThat(accountResponse.getId()).isNotNull().isEqualTo(accountId);
    assertThat(accountResponse.getPersonalIdentityNumber()).isEmpty();
    assertThat(accountResponse.getEmail()).isNotEmpty().get().isEqualTo(email);
    assertThat(accountResponse.getPhoneNumber()).isEmpty();
  }

  @Test
  void createAccountWithOptionalsReturnsSavedValues() {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        accountId,
        Optional.of(PERSONAL_IDENTITY_NUMBER),
        Optional.of(EMAIL),
        Optional.of(PHONE_NUMBER),
        deviceKeyDto);
    when(accountService.createAccount(any())).thenReturn(accountDto);

    var accountResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .personalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
            .email(EMAIL)
            .phoneNumber(PHONE_NUMBER)
            .deviceKey(deviceKeyRequest)
            .build())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(accountResponse).isNotNull();
    assertThat(accountResponse.getId()).isNotNull().isEqualTo(accountId);
    assertThat(accountResponse.getPersonalIdentityNumber()).isNotEmpty().get()
        .isEqualTo(PERSONAL_IDENTITY_NUMBER);
    assertThat(accountResponse.getEmail()).isNotEmpty().get().isEqualTo(EMAIL);
    assertThat(accountResponse.getPhoneNumber()).isNotEmpty().get().isEqualTo(PHONE_NUMBER);
    assertThat(accountResponse.getDeviceKey()).isEqualTo(toKeyResponse(deviceKeyRequest));
  }

  @Test
  void fetchingAccountWithBadUuidReturnsAccountIdProblem() {

    final var badFormattedAccountId = "12345-abcde";

    var problemResponse = client.get()
        .uri("/v0/accounts/{0}", badFormattedAccountId)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(problemResponse).isNotNull();
    assertThat(problemResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problemResponse.getTitle()).isNotEmpty();
    assertThat(problemResponse.getDetail()).isNotEmpty().hasValueSatisfying(detail -> {
      assertThat(detail).contains("'id'");
      assertThat(detail).contains("'%s'".formatted(badFormattedAccountId));
    });
    assertThat(problemResponse.getType()).isNotEmpty();
  }

  @Test
  void fetchingNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void servesAccountData() {

    final UUID accountId = UUID.randomUUID();
    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        accountId,
        Optional.of(PERSONAL_IDENTITY_NUMBER),
        Optional.of(EMAIL),
        Optional.of(PHONE_NUMBER),
        deviceKeyDto);
    when(accountService.getAccountById(eq(accountId))).thenReturn(Optional.of(accountDto));

    var accountResponse = client.get()
        .uri("/v0/accounts/{0}", accountId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(accountResponse).isNotNull();
    assertThat(accountResponse.getId()).isNotNull().isEqualTo(accountId);
    assertThat(accountResponse.getPersonalIdentityNumber()).isNotEmpty().get()
        .isEqualTo(PERSONAL_IDENTITY_NUMBER);
    assertThat(accountResponse.getEmail()).isNotEmpty().get().isEqualTo(EMAIL);
    assertThat(accountResponse.getPhoneNumber()).isNotEmpty().get().isEqualTo(PHONE_NUMBER);
    assertThat(accountResponse.getDeviceKey()).isEqualTo(toKeyResponse(deviceKeyRequest));
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

  public static class UnexpectedException extends RuntimeException {

    public UnexpectedException(String message) {
      super(message);
    }
  }
}
