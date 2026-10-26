-- SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
--
-- SPDX-License-Identifier: EUPL-1.2

CREATE TABLE accounts (
  id UUID PRIMARY KEY
);

-- One row per wallet instance bound to an account. The device key (the key of the device the
-- wallet runs on) is a public JWK stored as json; kid and RFC 7638 thumbprint are extracted for
-- lookup and uniqueness, so the same key cannot be registered twice under different kids.
CREATE TABLE wallets (
  id                    UUID PRIMARY KEY,
  account_id            UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
  device_key            JSONB NOT NULL,
  device_key_kid        VARCHAR(300) NOT NULL,
  device_key_thumbprint VARCHAR(64) NOT NULL,
  security_envelope     BYTEA,
  -- Assigned by wallet-r2ps when the wallet registers its HSM state. Can be replaced by wallets.id
  -- once r2ps lets the caller choose the client id.
  hsm_client_id         VARCHAR(255),
  CONSTRAINT wallets_device_key_kid_key UNIQUE (device_key_kid),
  CONSTRAINT wallets_device_key_thumbprint_key UNIQUE (device_key_thumbprint),
  CONSTRAINT wallets_hsm_client_id_key UNIQUE (hsm_client_id),
  CONSTRAINT chk_device_key_kid_min_length CHECK (LENGTH(device_key_kid) >= 1),
  -- The API limits the envelope to 65536 characters; a UTF-8 character is at most 4 bytes.
  CONSTRAINT chk_security_envelope_max_size CHECK (OCTET_LENGTH(security_envelope) <= 262144)
);

CREATE INDEX idx_wallets_account_id ON wallets (account_id);

COMMENT ON COLUMN wallets.security_envelope IS
  'Opaque device state owned by the client. Stored and returned as is, never interpreted.';
