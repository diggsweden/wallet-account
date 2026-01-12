// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import java.util.Optional;
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
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDto;
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
          assertThat(account.emailAdress()).isEqualTo(accountEntity.getEmailAdress());
          assertThat(account.publicKey()).isNotNull();
        });
  }

  @Test
  void saveAccount() {
    CreateAccountRequestDto requestDto =
        CreateAccountRequestDtoBuilder.builder()
            .emailAdress("none@your.businnes.se")
            .personalIdentityNumber("770101-1234")
            .telephoneNumber(Optional.of("070 123 123 12"))
            .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("nollnoll").build())
            .build();
    EntityExchangeResult<AccountDto> response = restClient.post()
        .uri("/account")
        .body(requestDto)
        .exchangeSuccessfully()
        .expectBody(AccountDto.class)
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
    CreateAccountRequestDto firstRequestDto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.businnes.se")
        .personalIdentityNumber("770101-1235")
        .telephoneNumber(Optional.of("070 123 123 12"))
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("99").build())
        .build();
    CreateAccountRequestDto secondRequestDto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.businnes.com")
        .personalIdentityNumber("770101-1235")
        .telephoneNumber(Optional.of("070 123 123 13"))
        .publicKey(TestUtils.publicKeyDtoBuilderWithDefaults("88").build())
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
    assertThat(firstResponse.getResponseBody().id())
        .isNotEqualTo(secondResponse.getResponseBody().id());

    EntityExchangeResult<AccountDto> response =
        restClient.get()
            .uri("/account/" + firstResponse.getResponseBody().id())
            .exchange()
            .expectBody(AccountDto.class)
            .returnResult();
    assertThat(response.getStatus().isSameCodeAs(HttpStatus.NOT_FOUND)).isTrue();

    EntityExchangeResult<AccountDto> response2 =
        restClient.get()
            .uri("/account/" + secondResponse.getResponseBody().id())
            .exchange()
            .expectBody(AccountDto.class)
            .returnResult();
    assertThat(response2.getStatus().is2xxSuccessful()).isTrue();


  }
}
