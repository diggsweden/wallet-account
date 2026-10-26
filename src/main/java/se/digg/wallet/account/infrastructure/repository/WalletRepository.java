// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import se.digg.wallet.account.infrastructure.model.WalletEntity;

public interface WalletRepository extends CrudRepository<WalletEntity, UUID> {
  boolean existsByDeviceKeyKidOrDeviceKeyThumbprint(String kid, String thumbprint);

  List<WalletEntity> findByAccountId(UUID accountId);
}
