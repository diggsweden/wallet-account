// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import se.digg.wallet.account.infrastructure.model.AccountEntity;


public interface AccountRepository extends CrudRepository<AccountEntity, UUID> {
  List<AccountEntity> findByPersonalIdentityNumber(String personalIdentityNumber);

}
