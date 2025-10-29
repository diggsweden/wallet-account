
package se.digg.wallet.account.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

@DataJpaTest
@Testcontainers
class AccountRepositoryTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @Autowired
  AccountRepository accountRepository;

  @Autowired
  TestEntityManager entityManager;

  @Test
  void testSaveAndRetriveAccount() {
    AccountEntity entity =
        new AccountEntity("770101-1234",
            "none@business.se",
            "070-123 123 1´23",
            new PublicKeyEntity("FDSFSDF2345fdsf",
                "hello world"));

    AccountEntity storedEntity = accountRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    AccountEntity foundEntity = accountRepository.findById(storedEntity.getId()).get();
    System.out.println("storedEntity " + storedEntity.toString());
    System.out.println("foundEntity " + foundEntity.toString());
    assertThat(foundEntity)
        .isNotNull()
        .isEqualTo(storedEntity);
    assertThat(foundEntity.getPublicKey())
        .isNotNull();
    assertThat(foundEntity.getPublicKey().getId()).isNotNull();


    System.out.println("result " + foundEntity.toString());
  }


}

