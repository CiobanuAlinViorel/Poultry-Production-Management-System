#!/bin/sh
set -e

SECRETS_DIR="infra/compose/secrets"
mkdir -p "$SECRETS_DIR"

PRIVATE_KEY="$SECRETS_DIR/jwt-private.pem"
PUBLIC_KEY="$SECRETS_DIR/jwt-public.pem"

if [ -f "$PRIVATE_KEY" ] && [ -f "$PUBLIC_KEY" ]; then
  echo "JWT keys already exist in $SECRETS_DIR. Remove them to regenerate."
  exit 0
fi

openssl genrsa -out "$PRIVATE_KEY" 2048
openssl rsa -in "$PRIVATE_KEY" -pubout -out "$PUBLIC_KEY"
chmod 600 "$PRIVATE_KEY"
chmod 644 "$PUBLIC_KEY"

echo "Generated JWT keys in $SECRETS_DIR"
echo "  private: $PRIVATE_KEY"
echo "  public:  $PUBLIC_KEY"
