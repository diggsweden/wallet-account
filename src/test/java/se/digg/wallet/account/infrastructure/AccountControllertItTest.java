// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.domain.model.ExtendedAccountDto;
import se.digg.wallet.account.api.origin.model.AccountDto;
import se.digg.wallet.account.api.origin.model.CreateAccountRequestDto;
import se.digg.wallet.account.api.origin.model.PublicKeyDto;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.repository.AccountRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class AccountControllertItTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @Autowired
  RestTestClient restClient;

  @Autowired
  AccountRepository repository;


  @Test
  void getAccount() {
    AccountEntity accountEntity = new AccountEntity(
        "770711-1234",
        "none@your.businnes.se",
        "070 123 12 12",
        null,
        TestUtils.generateJwkEntity(null),
        TestUtils.generateJwkEntity("1"));
    repository.save(accountEntity);
    EntityExchangeResult<AccountDto> response =
        restClient.get()
            .uri("/account/" + accountEntity.getId())
            .exchange()
            .expectBody(AccountDto.class)
            .returnResult();

    assertThat(response.getStatus().is2xxSuccessful()).isTrue();

    assertThat(response.getResponseBody())
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.getEmailAdress()).isEqualTo(accountEntity.getEmail());
          assertThat(account.getPublicKey()).isNotNull();
        });
  }

  @Test
  void saveAccount() {
    CreateAccountRequestDto requestDto =
        CreateAccountRequestDto.builder()
            .emailAdress("none@your.businnes.se")
            .personalIdentityNumber("770101-1234")
            .telephoneNumber("070 123 123 12")
            .publicKey(publicKeyDtoWithDefaults("nollnoll"))
            .build();
    EntityExchangeResult<ExtendedAccountDto> response = restClient.post()
        .uri("/account")
        .body(requestDto)
        .exchangeSuccessfully()
        .expectBody(ExtendedAccountDto.class)
        .returnResult();

    assertThat(response.getStatus().is2xxSuccessful()).isTrue();

    assertThat(response.getResponseBody())
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.id()).isNotNull();
        });
  }

  @Test
  void testSaveDuplicateAccounts() {
    CreateAccountRequestDto firstRequestDto = CreateAccountRequestDto.builder()
        .emailAdress("none@your.businnes.se")
        .personalIdentityNumber("770101-1235")
        .telephoneNumber("070 123 123 12")
        .publicKey(publicKeyDtoWithDefaults("99"))
        .build();
    CreateAccountRequestDto secondRequestDto = CreateAccountRequestDto.builder()
        .emailAdress("none@your.businnes.com")
        .personalIdentityNumber("770101-1235")
        .telephoneNumber("070 123 123 13")
        .publicKey(publicKeyDtoWithDefaults("88"))
        .build();
    EntityExchangeResult<AccountDto> firstResponse =
        restClient.post()
            .uri("/account")
            .body(firstRequestDto)
            .exchange()
            .expectBody(AccountDto.class)
            .returnResult();
    EntityExchangeResult<AccountDto> secondResponse =
        restClient.post()
            .uri("/account")
            .body(secondRequestDto)
            .exchange()
            .expectBody(AccountDto.class)
            .returnResult();
    assertThat(firstResponse.getStatus().is2xxSuccessful()).isTrue();
    assertThat(secondResponse.getStatus().is2xxSuccessful()).isTrue();
    assertThat(firstResponse.getResponseBody()).isNotNull();
    assertThat(secondResponse.getResponseBody()).isNotNull();
    assertThat(firstResponse.getResponseBody().getId())
        .isNotEqualTo(secondResponse.getResponseBody().getId());

    EntityExchangeResult<ExtendedAccountDto> response =
        restClient.get()
            .uri("/account/" + firstResponse.getResponseBody().getId())
            .exchange()
            .expectBody(ExtendedAccountDto.class)
            .returnResult();
    assertThat(response.getStatus().isSameCodeAs(HttpStatus.NOT_FOUND)).isTrue();

    EntityExchangeResult<ExtendedAccountDto> response2 =
        restClient.get()
            .uri("/account/" + secondResponse.getResponseBody().getId())
            .exchange()
            .expectBody(ExtendedAccountDto.class)
            .returnResult();
    assertThat(response2.getStatus().is2xxSuccessful()).isTrue();
  }

  private static PublicKeyDto publicKeyDtoWithDefaults(String kid) {
    return PublicKeyDto.builder()
        .kty("EC")
        .crv("P-256")
        .x("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4")
        .y("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM")
        .alg("alg")
        .use("enc")
        .kid(kid)
        .build();
  }
}
