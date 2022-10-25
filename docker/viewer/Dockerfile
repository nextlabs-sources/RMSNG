FROM skydrm-base:LATEST
MAINTAINER Nextlabs
ENV CATALINA_OPTS $CATALINA_OPTS -Djava.locale.providers=COMPAT,CLDR -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true
COPY xlib/embeddedpdp/ /var/opt/nextlabs/rms/datafiles/javapc/
RUN ln -sf /var/opt/nextlabs/rms/shared/conf/log4j2_viewer.xml /var/opt/nextlabs/rms/datafiles/javapc/config/log4j2.xml
RUN mkdir -p /var/opt/nextlabs/rms/datafiles/javapc/jservice
COPY xlib/libGLU/mesa-libGLU-9.0.0-4.el7.x86_64.rpm /tmp/libGLU/mesa-libGLU-9.0.0-4.el7.x86_64.rpm
COPY xlib/libXvfb/xdmcp/libXdmcp-1.1.2-6.el7.x86_64.rpm /tmp/libXvfb/libXdmcp-1.1.2-6.el7.x86_64.rpm
COPY xlib/libXvfb/xfont2/libXfont2-2.0.3-1.el7.x86_64.rpm /tmp/libXvfb/libXfont2-2.0.3-1.el7.x86_64.rpm
COPY xlib/libXvfb/xorg-x11-server-common/xkb/libxkbfile-1.0.9-3.el7.x86_64.rpm /tmp/libXvfb/libxkbfile-1.0.9-3.el7.x86_64.rpm
COPY xlib/libXvfb/xorg-x11-server-common/xkb/xorg-x11-xkb-utils-7.7-14.el7.x86_64.rpm /tmp/libXvfb/xorg-x11-xkb-utils-7.7-14.el7.x86_64.rpm
COPY xlib/libXvfb/xorg-x11-server-common/xkeyboard-config/xkeyboard-config-2.24-1.el7.noarch.rpm /tmp/libXvfb/xkeyboard-config-2.24-1.el7.noarch.rpm
COPY xlib/libXvfb/xorg-x11-server-common/xorg-x11-server-common-1.20.4-15.el7_9.x86_64.rpm /tmp/libXvfb/xorg-x11-server-common-1.20.4-15.el7_9.x86_64.rpm
COPY xlib/libXvfb/xorg-x11-xauth/xorg-x11-xauth-1.0.9-1.el7.x86_64.rpm /tmp/libXvfb/xorg-x11-xauth-1.0.9-1.el7.x86_64.rpm
COPY xlib/libXvfb/xorg-x11-server-Xvfb-1.20.4-15.el7_9.x86_64.rpm /tmp/libXvfb/xorg-x11-server-Xvfb-1.20.4-15.el7_9.x86_64.rpm
RUN yum update -y && yum install -y libXext-1.3.3-3.el7.x86_64 libXrender vim pixman libXmu
RUN yum -y localinstall /tmp/libGLU/mesa-libGLU-9.0.0-4.el7.x86_64.rpm && rm -f /tmp/libGLU/mesa-libGLU-9.0.0-4.el7.x86_64.rpm
RUN yum -y localinstall /tmp/libXvfb/libXdmcp-1.1.2-6.el7.x86_64.rpm \
/tmp/libXvfb/libXfont2-2.0.3-1.el7.x86_64.rpm \
/tmp/libXvfb/libxkbfile-1.0.9-3.el7.x86_64.rpm \
/tmp/libXvfb/xorg-x11-xkb-utils-7.7-14.el7.x86_64.rpm \
/tmp/libXvfb/xkeyboard-config-2.24-1.el7.noarch.rpm \
/tmp/libXvfb/xorg-x11-server-common-1.20.4-15.el7_9.x86_64.rpm \
/tmp/libXvfb/xorg-x11-xauth-1.0.9-1.el7.x86_64.rpm \
/tmp/libXvfb/xorg-x11-server-Xvfb-1.20.4-15.el7_9.x86_64.rpm
RUN rm -rf /tmp/libXvfb
RUN mkdir -p /opt/nextlabs/rms/viewer/external/ /var/opt/nextlabs/rms/datafiles/ && \
chmod -R 777 /opt/nextlabs /var/opt/nextlabs && \
chown -R tomcat:tomcat /opt/nextlabs /var/opt/nextlabs
ENV LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/nextlabs/rms/viewer/external/perceptive \
    ISYS_FONTS=/opt/nextlabs/rms/viewer/external/perceptive/fonts
COPY viewer/build/libs/viewer.war /opt/nextlabs/rms/tomcat/webapps
COPY docker/viewer/entrypoint.sh /
RUN chmod 755 /entrypoint.sh
USER tomcat
ENTRYPOINT ["/entrypoint.sh"]
CMD ["run"]
