# RedHAT UBI 7.8 amd64 based container with Java and Tomcat
FROM registry.access.redhat.com/ubi7/ubi
MAINTAINER Nextlabs

# Install prepare infrastructure
RUN yum -y update && \
 yum -y install wget tar unzip net-tools fontconfig rng-tools

# Prepare environment 
RUN mkdir -p /opt/nextlabs/rms
RUN mkdir -p /var/opt/nextlabs/rms/shared
ENV RMS_INSTALL_DIR /opt/nextlabs/rms
ENV RMS_SHARED_DIR /var/opt/nextlabs/rms/shared
ENV JAVA_HOME $RMS_INSTALL_DIR/java
ENV CATALINA_HOME $RMS_INSTALL_DIR/tomcat 
ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/bin:$CATALINA_HOME/scripts
ENV LC_ALL en_US.UTF-8

# Copy requisite dependencies (JRE/Tomcat)
# These dependencies should have been already set up outside before
#   docker build command is initiated.
# UnlimitedJCEPolicy jars should have been copied in build job itself
COPY xlib/jdk-11.0.2+7-jre/ ${JAVA_HOME}/
COPY xlib/tomcat_8.5.35 ${CATALINA_HOME}/

RUN chmod +x ${CATALINA_HOME}/bin/*.sh
RUN chmod -R +x ${JAVA_HOME}/

# Create tomcat user
RUN groupadd -r tomcat && \
 useradd -g tomcat -d ${CATALINA_HOME} -s /sbin/nologin  -c "Tomcat user" tomcat && \
 chown -R tomcat:tomcat ${CATALINA_HOME} ${JAVA_HOME} && \
 chmod u+w ${JAVA_HOME}/lib/security/cacerts

COPY docker/base/tomcat/conf/ $CATALINA_HOME/conf/
COPY docker/base/tomcat/bin/ $CATALINA_HOME/bin/
EXPOSE 8443
