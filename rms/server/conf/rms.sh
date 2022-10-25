#!/bin/bash

########################################################################
# Example:
#       ./rms.sh --inWebContainer --cmd=Init --keystore=iCA.bks --passwordFile=iCA.password
#
########################################################################

if [ -z "$CATALINA_HOME" ]; then
    CATALINA_HOME=/var/lib/tomcat7
fi

pushd . > /dev/null
cd "$CATALINA_HOME" > /dev/null

if [ "$OSTYPE" == "msys" ]; then
    CATALINA_HOME=${CATALINA_HOME:1:1}:${CATALINA_HOME:2}
    java -cp "$CATALINA_HOME/webapps/rms/WEB-INF/classes;$CATALINA_HOME/webapps/rms/WEB-INF/lib/*;$CATALINA_HOME/lib/*" com.nextlabs.rms.Main "$@"
else
    java -cp "$CATALINA_HOME/webapps/rms/WEB-INF/classes:$CATALINA_HOME/webapps/rms/WEB-INF/lib/*:/usr/share/tomcat7/lib/*" com.nextlabs.rms.Main "$@"
fi
popd > /dev/null
