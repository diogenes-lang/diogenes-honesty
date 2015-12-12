if [[ $# -ne 1 && $# -ne 2 ]]; then
	echo "Usage: $0 <username> [<password>]"
	exit 1;
fi

USERNAME=$1
DOMAIN="co2.unica.it"

if [[ $# -eq 1 ]]; then
	PASSWORD=$USERNAME
else
	PASSWORD=$2
fi

USERNAME=$USERNAME@$DOMAIN


URL=http://co2.unica.it:8080/middleware/api/user/create


echo "Creating CO2 user"
echo "USERNAME: $USERNAME"
echo "PASSWORD: $PASSWORD"

PASSWORD=`echo -n "$PASSWORD" | sha256sum | cut -d\  -f 1`
JSON={\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}


echo "PASSWORD (hashed): $PASSWORD"
echo "JSON: $JSON"

curl -H "Content-Type: application/json" -X POST -d "$JSON" $URL

echo ""
