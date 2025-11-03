// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
class AccountControllertItTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @Autowired
  TestRestTemplate restTemplate;

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
    ResponseEntity<AccountDto> response =
        restTemplate.getForEntity("/account/" + accountEntity.getId(), AccountDto.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    assertThat(response.getBody())
        .isNotNull()
        .satisfies(account -> {
          assertThat(account.emailAdress()).isEqualTo(accountEntity.getEmailAdress());
          assertThat(account.jwk()).isNotNull();
        });
  }

  @Test
  void saveAccount() {
    CreateAccountRequestDto requestDto =
        CreateAccountRequestDtoBuilder.builder()
            .emailAdress("none@your.businnes.se")
            .personalIdentityNumber("770101-1234")
            .telephoneNumber(Optional.of("070 123 123 12"))
            .jwk(TestUtils.generateJwkDto("nollnoll"))
            .build();
    ResponseEntity<AccountDto> response =
        restTemplate.postForEntity("/account", requestDto, AccountDto.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    assertThat(response.getBody())
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
        .jwk(TestUtils.generateJwkDto("99"))
        .build();
    CreateAccountRequestDto secondRequestDto = CreateAccountRequestDtoBuilder.builder()
        .emailAdress("none@your.businnes.com")
        .personalIdentityNumber("770101-1235")
        .telephoneNumber(Optional.of("070 123 123 13"))
        .jwk(TestUtils.generateJwkDto("88"))
        .build();
    ResponseEntity<AccountDto> firstResponse =
        restTemplate.postForEntity("/account", firstRequestDto, AccountDto.class);
    ResponseEntity<AccountDto> secondResponse =
        restTemplate.postForEntity("/account", secondRequestDto, AccountDto.class);
    assertThat(firstResponse.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(firstResponse.getBody()).isNotNull();
    assertThat(secondResponse.getBody()).isNotNull();
    assertThat(firstResponse.getBody().id()).isNotEqualTo(secondResponse.getBody().id());

    ResponseEntity<AccountDto> response =
        restTemplate.getForEntity("/account/" + firstResponse.getBody().id(), AccountDto.class);
    assertThat(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)).isTrue();
    ResponseEntity<AccountDto> response2 =
        restTemplate.getForEntity("/account/" + secondResponse.getBody().id(), AccountDto.class);
    assertThat(response2.getStatusCode().is2xxSuccessful()).isTrue();


  }
}
