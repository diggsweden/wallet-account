// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.model;

/** The account with its wallet, and whether the put stored the wallet or found it stored. */
public record PutWalletResult(AccountDto account, boolean created) {
}
