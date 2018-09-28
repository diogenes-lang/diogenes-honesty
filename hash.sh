if [[ $# -ne 1 ]]; then
	echo "Usage: $0 <password>"
	exit 1;
fi

PASSWORD=$1
PWDHASH=`echo -n "$PASSWORD" | openssl dgst -sha256 | cut -f 2 -d\ `
PWDHASH=`echo -n "$PWDHASH" | openssl dgst -sha256 | cut -f 2 -d\ `

echo "Plain password: $PASSWORD"
echo "Hashed password: $PWDHASH"

