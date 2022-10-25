#!/bin/bash

########################################################################
# Example:
#	./accessKeyMgmt.sh --list
#	./accessKeyMgmt.sh --create --name=ABCD 
#	./accessKeyMgmt.sh --revoke --name=ABCD
#	./accessKeyMgmt.sh --refresh --name=ABCD
#	./accessKeyMgmt.sh --joinTenant --name=ABCD --tenantName=skydrm.com
#
########################################################################

if [ -z "$RMS_INSTALL_DIR" ]; then
    echo "RMS_INSTALL_DIR is not set"
	exit 1
else
	if [ ! -d "$RMS_INSTALL_DIR/external/tomcat" ]; then
		echo "Tomcat folder does not exist in $RMS_INSTALL_DIR/external/tomcat"
		exit 1
	else
		CATALINA_HOME="$RMS_INSTALL_DIR/external/tomcat"
	fi
	
	if [ ! -d "$RMS_INSTALL_DIR/external/jre" ]; then
		echo "JRE folder does not exist in $RMS_INSTALL_DIR/external/jre"
		exit 1
	else
		JAVA_HOME="$RMS_INSTALL_DIR/external/jre/bin"
	fi
	
fi

pushd . > /dev/null
cd "$CATALINA_HOME" > /dev/null
if [ "$OSTYPE" == "msys" ]; then
    "$JAVA_HOME/java" -cp "$CATALINA_HOME/webapps/rms/WEB-INF/classes;$CATALINA_HOME/webapps/rms/WEB-INF/lib/*;$CATALINA_HOME/lib/*" com.nextlabs.rms.Main --cmd=AccessKeyMgmt "$@" 2>/dev/null
else
    "$JAVA_HOME/java" -cp "$CATALINA_HOME/webapps/rms/WEB-INF/classes:$CATALINA_HOME/webapps/rms/WEB-INF/lib/*:$CATALINA_HOME/lib/*" com.nextlabs.rms.Main --cmd=AccessKeyMgmt "$@" 2>/dev/null
fi
popd > /dev/null

