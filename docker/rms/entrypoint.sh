#!/bin/bash
set -x

KEYSTORE="$JAVA_HOME/lib/security/cacerts"
KEYSTORE_PASS="changeit"
KEYTOOL="$JAVA_HOME/bin/keytool"
ENROLLCERTS="/var/opt/nextlabs/rms/shared/enroll/cer"
CUSTOM_SCRIPT_DIR="/var/opt/nextlabs/rms/shared/conf"

# Check if customers have their own script. If they do, run their script
if [[ -f $CUSTOM_SCRIPT_DIR/custom_script_rms.sh ]]
then
	sh $CUSTOM_SCRIPT_DIR/custom_script_rms.sh
fi

for cerfile in "$ENROLLCERTS"/*.cer
do
  if [ -f "$cerfile" ]
  then
    filename=$(basename "$cerfile")
	filename="${filename%.*}"
	echo "Checking $KEYSTORE to add $cerfile with alias $filename"

	"$KEYTOOL" -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" -list -alias "$filename" > /dev/null 2>&1
	if [ $? -eq 0 ]
	then
		echo "Alias of $filename already found, skipping insertion into $KEYSTORE"
	else
		echo "Inserting into $KEYSTORE under alias of $filename"
		"$KEYTOOL" -noprompt -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" -import -trustcacerts -alias "$filename" -file "$cerfile"
	fi
  fi
done

# Run tomcat if the first argument is run otherwise try to run whatever the argument is a command
if [ "$1" = 'run' ]; then
  ##
  # Check if server.xml is present in datafiles
  ##
  [ -f /var/opt/nextlabs/rms/shared/tomcat/conf/server.xml ] && \
      cp -f /var/opt/nextlabs/rms/shared/tomcat/conf/* $CATALINA_HOME/conf/
  echo "run tomcat"
  exec catalina.sh "$@"
else
  exec "$@"
fi

