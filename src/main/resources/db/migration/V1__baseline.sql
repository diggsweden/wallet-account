-- SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
--
-- SPDX-License-Identifier: EUPL-1.2

CREATE TABLE public_key (
  id  UUID PRIMARY KEY,
  kty VARCHAR(30),
  kid VARCHAR(300) NOT NULL,
  alg VARCHAR(30),
  use VARCHAR(30),
  crv VARCHAR(30),
  x   VARCHAR(300),
  y   VARCHAR(300),
  CONSTRAINT public_key_kid_key UNIQUE (kid),
  CONSTRAINT chk_kid_min_length CHECK (LENGTH(kid) >= 1)
);

CREATE INDEX idx_public_key_kid ON public_key (kid);

CREATE TABLE accounts (
  id                UUID PRIMARY KEY,
  device_key_id     UUID,
  security_envelope OID,
  hsm_client_id     VARCHAR(255)
);
