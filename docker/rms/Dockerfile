FROM skydrm-base:LATEST
MAINTAINER Nextlabs
ENV CATALINA_OPTS $CATALINA_OPTS -Djava.locale.providers=COMPAT,CLDR -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true
COPY xlib/embeddedpdp/ /var/opt/nextlabs/rms/datafiles/javapc/
RUN ln -sf /var/opt/nextlabs/rms/shared/conf/log4j2_rms.xml /var/opt/nextlabs/rms/datafiles/javapc/config/log4j2.xml
RUN mkdir -p /var/opt/nextlabs/rms/datafiles/javapc/jservice
COPY docker/rms/datafiles/ /var/opt/nextlabs/rms/datafiles/ 
RUN chown -R tomcat:tomcat  /var/opt/nextlabs/ && \
    chmod -R 755 /var/opt/nextlabs/ 
COPY rms/server/build/libs/rms.war /opt/nextlabs/rms/tomcat/webapps
COPY docker/rms/apple-app-site-association /opt/nextlabs/rms/tomcat/webapps/ROOT
COPY docker/rms/entrypoint.sh /
RUN chmod 755 /entrypoint.sh
USER tomcat
ENTRYPOINT ["/entrypoint.sh"]
CMD ["run"]
