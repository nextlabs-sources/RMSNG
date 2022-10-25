#!/bin/bash

########################################################################
# Example:
#	./router.sh --cmd=Init --keystore=rootCA.bks --passwordFile=rootCA.passwd
#	./router.sh --cmd=Tenant --operation=add --name=skydrm.com --server=https://www.skydrm.com/rms
#	./router.sh --cmd=Tenant --operation=add --name=jt2go --server=https://www.skydrm.com/rms
#	./router.sh --cmd=Tenant --operation=add --name=skydrm.com --server=https://rmtest.nextlabs.solutions/rms
#	./router.sh --cmd=Tenant --operation=add --name=jt2go --server=https://rmtest.nextlabs.solutions/rms
#	./router.sh --cmd=Tenant --operation=reset-otp --name=skydrm.com
#
########################################################################

if [ -z "$CATALINA_HOME" ]; then
    CATALINA_HOME=/var/lib/tomcat7
fi

pushd . > /dev/null
cd "$CATALINA_HOME" > /dev/null
if [ "$OSTYPE" == "msys" ]; then
    CATALINA_HOME=${CATALINA_HOME:1:1}:${CATALINA_HOME:2}
    java -cp "$CATALINA_HOME/webapps/router/WEB-INF/classes;$CATALINA_HOME/webapps/router/WEB-INF/lib/*;$CATALINA_HOME/lib/*" com.nextlabs.router.Main "$@"
else
    java -cp "$CATALINA_HOME/webapps/router/WEB-INF/classes:$CATALINA_HOME/webapps/router/WEB-INF/lib/*:/usr/share/tomcat7/lib/*" com.nextlabs.router.Main "$@"
fi
popd > /dev/null

