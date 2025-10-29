package se.digg.wallet.account.infrastructure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.CreateAccountRequestDtoBuilder;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;
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
                new PublicKeyEntity("mykeyValue", "MyKeyId"));
        repository.save(accountEntity);
        ResponseEntity<AccountDto> response =
                restTemplate.getForEntity("/account/" + accountEntity.getId(), AccountDto.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(response.getBody())
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
                        .publicKey(new PublicKeyDto("dsadfewfewfw", "keyId"))
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
}
