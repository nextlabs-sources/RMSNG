FROM skydrm-base:LATEST
MAINTAINER Nextlabs
COPY docker/router/datafiles/* /var/opt/nextlabs/rms/datafiles/
RUN chmod -R 755  /var/opt/nextlabs/ && \
chown -R tomcat:tomcat /var/opt/nextlabs/
COPY router/server/build/libs/router.war /opt/nextlabs/rms/tomcat/webapps
COPY docker/router/entrypoint.sh /
RUN chmod 755 /entrypoint.sh
USER tomcat
ENTRYPOINT ["/entrypoint.sh"]
CMD ["run"]
