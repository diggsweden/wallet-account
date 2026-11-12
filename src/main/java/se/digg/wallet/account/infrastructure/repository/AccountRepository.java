// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import se.digg.wallet.account.infrastructure.model.AccountEntity;


public interface AccountRepository extends CrudRepository<AccountEntity, UUID> {

  /** Stores an account id assigned by the authorization server, unless it is already stored. */
  @Modifying
  @Query(value = "INSERT INTO accounts (id) VALUES (:id) ON CONFLICT DO NOTHING",
      nativeQuery = true)
  void insertIfAbsent(UUID id);

  /** Locks the account until the transaction ends, so changes to its wallets run one at a time. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM AccountEntity a WHERE a.id = :id")
  Optional<AccountEntity> findByIdForUpdate(UUID id);
}
