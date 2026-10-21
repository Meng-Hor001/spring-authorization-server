#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
key_path="${SIGNING_KEY_PATH:-.local/signing.p12}"
export SIGNING_KEY_PASSWORD="${SIGNING_KEY_PASSWORD:-local-signing-password}"
key_alias="${SIGNING_KEY_ALIAS:-auth-signing}"

if [[ -e "$key_path" ]]; then
  echo "Signing key already exists; leaving it unchanged: $key_path"
  exit 0
fi

umask 077
mkdir -p "$(dirname "$key_path")"
keytool -genkeypair -alias "$key_alias" -keyalg RSA -keysize 3072 \
  -storetype PKCS12 -keystore "$key_path" \
  -storepass:env SIGNING_KEY_PASSWORD \
  -dname "CN=Local Authorization Server" -validity 3650 -noprompt
echo "Created local signing key: $key_path"
