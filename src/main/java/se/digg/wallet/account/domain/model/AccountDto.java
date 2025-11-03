// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import java.util.Optional;
import java.util.UUID;
import se.digg.wallet.account.application.model.JwkDto;

@RecordBuilder
public record AccountDto(
    UUID id,
    String personalIdentityNumber,
    String emailAdress,
    Optional<String> telephoneNumber,
    JwkDto jwk) {
}
