#!/bin/bash
set -x

KEYSTORE="$JAVA_HOME/lib/security/cacerts"
KEYSTORE_PASS="changeit"
KEYTOOL="$JAVA_HOME/bin/keytool"
ENROLLCERTS="/var/opt/nextlabs/rms/shared/enroll/cer"
CUSTOM_SCRIPT_DIR="/var/opt/nextlabs/rms/shared/conf"

# Check if customers have their own script. If they do, run their script
if [[ -f $CUSTOM_SCRIPT_DIR/custom_script_viewer.sh ]] 
then
  sh $CUSTOM_SCRIPT_DIR/custom_script_viewer.sh
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
  # Check if Docviewer.zip is present - if so, unpack it and set appropriate file perms
  # Hopefully there's only at most ONE DocViewer zip - installation team should take care of this
  ##
  for file in /opt/nextlabs/rms/viewer/viewers/RightsManagementServer-DocViewer-*.zip
  do
    rm -rf /opt/nextlabs/rms/viewer/external/perceptive ; mkdir -p /opt/nextlabs/rms/viewer/external/perceptive
    UNPACK_TMP=$(mktemp -d)
    cd ${UNPACK_TMP} ; unzip ${file}
    mv ISYS11df.jar RMS-Perceptive-Lib.jar linux/intel-64/
    cd linux/intel-64
    basename -z ${file} > rms-perceptive-version.txt
    chmod 755 fonts ; chmod ugo+r fonts/*
    chmod 755 ISYS11df.jar isys_doc2text ISYSreadershd ISYSreadersocr.dat isys_threadtest libISYS11dfjava.so libISYS11df.so libISYSautocad.so libISYSgraphics.so libISYSpdf6.so libISYSreadershd.so libISYSreadersocr.so libISYSreaders.so libISYSshared.so RMS-Perceptive-Lib.jar rms-perceptive-version.txt
    tar cf - . | ( cd /opt/nextlabs/rms/viewer/external/perceptive ; tar xf - )
    cd ; rm -rf ${UNPACK_TMP} ; unset UNPACK_TMP
  done
  [ -f /var/opt/nextlabs/rms/shared/tomcat/conf/server.xml ] && \
      cp -f /var/opt/nextlabs/rms/shared/tomcat/conf/* $CATALINA_HOME/conf/
  echo "run tomcat"
  exec catalina.sh "$@"
else
  exec "$@"
fi
