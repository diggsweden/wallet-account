package se.digg.wallet.account.infrastructure.repository;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import se.digg.wallet.account.infrastructure.model.AccountEntity;

public interface AccountRepository extends CrudRepository<AccountEntity, UUID> {

}
