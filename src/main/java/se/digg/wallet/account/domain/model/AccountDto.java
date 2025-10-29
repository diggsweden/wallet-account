package se.digg.wallet.account.domain.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import java.util.Optional;
import java.util.UUID;
import se.digg.wallet.account.application.model.PublicKeyDto;

@RecordBuilder
public record AccountDto(
    UUID id,
    String personalIdentityNumber,
    String emailAdress,
    Optional<String> telephoneNumber,
    PublicKeyDto publicKey) {
}
