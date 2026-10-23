-- SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
--
-- SPDX-License-Identifier: EUPL-1.2

-- The device key is a public JWK stored as json; kid and RFC 7638 thumbprint are extracted for
-- lookup and uniqueness, so the same key cannot be registered twice under different kids.
CREATE TABLE accounts (
  id                    UUID PRIMARY KEY,
  device_key            JSONB NOT NULL,
  device_key_kid        VARCHAR(300) NOT NULL,
  device_key_thumbprint VARCHAR(64) NOT NULL,
  security_envelope     BYTEA,
  hsm_client_id         VARCHAR(255),
  CONSTRAINT accounts_device_key_kid_key UNIQUE (device_key_kid),
  CONSTRAINT accounts_device_key_thumbprint_key UNIQUE (device_key_thumbprint),
  CONSTRAINT chk_device_key_kid_min_length CHECK (LENGTH(device_key_kid) >= 1),
  -- The API limits the envelope to 65536 characters; a UTF-8 character is at most 4 bytes.
  CONSTRAINT chk_security_envelope_max_size CHECK (OCTET_LENGTH(security_envelope) <= 262144)
);

COMMENT ON COLUMN accounts.security_envelope IS
  'Opaque device state owned by the client. Stored and returned as is, never interpreted.';
