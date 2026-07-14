// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import jakarta.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
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
import se.digg.wallet.account.api.v0.model.AccountRequest;
import se.digg.wallet.account.api.v0.model.AccountResponse;
import se.digg.wallet.account.api.v0.model.KeyRequest;
import se.digg.wallet.account.api.v0.model.KeyResponse;
import se.digg.wallet.account.api.v0.model.ProblemParameterResponse;
import se.digg.wallet.account.api.v0.model.ProblemResponse;
import se.digg.wallet.account.application.exception.AccountAlreadyExistsException;
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
  private static final String ALREADY_EXISTS = "/problem-details/resource-already-exists";
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
  void setUp(WebApplicationContext context) { // Inject the configuration
    client = RestTestClient.bindToApplicationContext(context).build();
    MDC.put("transactionId", TRANSACTION_ID);
  }

  @AfterEach
  void cleanUp() {
    MDC.clear();
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

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST, VALIDATION_FAILURE, "deviceKey");
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

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST, VALIDATION_FAILURE,
        invalidProperty);
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

    client.post()
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

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST, VALIDATION_FAILURE, "email");
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

    assertProblemDetails(problemResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void createDuplicateAccountReturnsAccountAlreadyExistsProblem() {

    var alreadyExistsException = new AccountAlreadyExistsException("the-message");
    when(accountService.createAccount(any())).thenThrow(alreadyExistsException);

    var conflict = HttpStatus.CONFLICT;
    var problemResponse = client.post()
        .uri("/v0/accounts")
        .body(AccountRequest.builder()
            .deviceKey(defaultKeyRequest().build())
            .build())
        .exchange()
        .expectStatus()
        .isEqualTo(conflict)
        .expectBody(ProblemResponse.class)
        .returnResult()
        .getResponseBody();

    assertProblemDetails(problemResponse, conflict, ALREADY_EXISTS, null);
  }

  @Test
  void createAccountWithoutOptionalsReturnsSavedValues() {

    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        ACCOUNT_ID,
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
    assertThat(accountResponse.getId()).isEqualTo(ACCOUNT_ID);
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

    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        ACCOUNT_ID,
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
    assertThat(accountResponse.getId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(accountResponse.getPersonalIdentityNumber()).isEmpty();
    assertThat(accountResponse.getEmail()).isNotEmpty().get().isEqualTo(email);
    assertThat(accountResponse.getPhoneNumber()).isEmpty();
  }

  @Test
  void createAccountWithOptionalsReturnsSavedValues() {

    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        ACCOUNT_ID,
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
    assertThat(accountResponse.getId()).isNotNull().isEqualTo(ACCOUNT_ID);
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

    assertProblemDetails(problemResponse, HttpStatus.BAD_REQUEST);
    assertThat(problemResponse.getDetail()).isNotEmpty().hasValueSatisfying(detail -> {
      assertThat(detail).contains("'id'");
      assertThat(detail).contains("'%s'".formatted(badFormattedAccountId));
    });
  }

  @Test
  void fetchingNonExistingAccountReturnsNotFound() {

    when(accountService.getAccountById(any())).thenReturn(Optional.empty());

    client.get()
        .uri("/v0/accounts/{0}", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void servesAccountData() {

    final KeyRequest deviceKeyRequest = defaultKeyRequest().build();
    final PublicKeyDto deviceKeyDto = toPublicKeyDto(deviceKeyRequest);

    var accountDto = new AccountDto(
        ACCOUNT_ID,
        Optional.of(PERSONAL_IDENTITY_NUMBER),
        Optional.of(EMAIL),
        Optional.of(PHONE_NUMBER),
        deviceKeyDto);
    when(accountService.getAccountById(eq(ACCOUNT_ID))).thenReturn(Optional.of(accountDto));

    var accountResponse = client.get()
        .uri("/v0/accounts/{0}", ACCOUNT_ID)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(accountResponse).isNotNull();
    assertThat(accountResponse.getId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(accountResponse.getPersonalIdentityNumber()).isNotEmpty().get()
        .isEqualTo(PERSONAL_IDENTITY_NUMBER);
    assertThat(accountResponse.getEmail()).isNotEmpty().get().isEqualTo(EMAIL);
    assertThat(accountResponse.getPhoneNumber()).isNotEmpty().get().isEqualTo(PHONE_NUMBER);
    assertThat(accountResponse.getDeviceKey()).isEqualTo(toKeyResponse(deviceKeyRequest));
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

  public static class UnexpectedException extends RuntimeException {

    public UnexpectedException(String message) {
      super(message);
    }
  }

  private static void assertProblemDetails(ProblemResponse problemResponse,
      HttpStatus expectedHttpStatus) {

    assertProblemDetails(problemResponse, expectedHttpStatus, null, null);
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
