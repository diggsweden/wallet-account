// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotEmpty;
import java.util.Optional;

@RecordBuilder
public record CreateAccountRequestDto(
    @NotEmpty String personalIdentityNumber,
    @NotEmpty String emailAdress,
    Optional<String> telephoneNumber,
    JwkDto jwk) {

}
