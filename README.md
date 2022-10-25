# Right Management Server

## How to read/edit this file
To read this file:

- Open Chrome
- Install "Markdown Preview Plus" Chrome extension
- Check "Allow access file URLs" in extension settings.
- Drag this file into Chrome

You can use "Minimalist Markdown Editor" in Chrome extension, to edit this file.

## Overview
This repo contains of following modules:

- **shared** A set of common utility classes shared across other modules.
- **router** A central router that routing request between all RMS servers.
	- **database**	A database access layer for router module.
	- **server** Web application for router module.
- **rms** Right management server
	- **database**	A database access layer for rms module.
	- **server** Web application for rms module.
	- **pql** A library project to parse PQL.
	- **saml2** A library project to handle SAML2 protocol.
	- **repository** A library project processing request to access Google Drive, Onedrive and Dropbox.
- **client** Right management client simulator
- **viewer**


## Build instruction

### Prerequisites
Note: You must use **NTFS** file system if you are using Windows.
Please read [Setup Development Environment](https://bitbucket.org/nxtlbs-devops/rightsmanagement-wiki/wiki/RMS/setup/Setup%20Windows%20Development%20Environment) for how to install required tools on each platform.

- bash (MinGW for Windows)
- git
- Ant
- Tomcat 8
- findbugs 3.0.1
- PostgreSQL
- JDK8
- JCE Unlimited Strength Jurisdiction Policy

### Copy over the dependencies
The gradle build process depends on the following libraries: 

- policy controller : The current build as of 2018-07-05 depends on version 0.97-rc12-20110504-1459 of the embedded PDP. This is normally pulled by Gradle from the artifact repository during the Jenkins build. For local development on your workstation you can copy the "xlib" directory from the network share at \\semakau\share\Teams\Secure Collaboration\RMSNG into the rmsng source root directory. The "xlib" directory in your source tree should appear at the same level as the rms, router and viewer directories. 
- policy controller for runtime : [check if this is really needed] If not already present, create the directory "C:\ProgramData\NextLabs\RMS\datafiles\javapc" and copy over the contents of the embeddedpdp directory here. Now the "C:\ProgramData\NextLabs\RMS\datafiles\javapc" directory should also contain the `config`, `decryptj` and `logs` directories with the `decrypt.bat`, `decrypt.sh` and `embeddedpdp.jar` files. 

### Config build environment
There is a config.xml.sample file in RMSNG foler, copy this file into config.xml and edit config.xml to match your environment:
```
$ cp config.xml.sample config.xml
$ vi config.xml
```

For example, you may edit your config.xml file to point to the correct locations of your findbugs and tomcat directories as follows: 
```
<?xml version="1.0" encoding="UTF-8"?>
<project name="config">
    <!--
    <property name="developer" value="true"/>
    <property name="lib_home" location="C:/work/nextlabs/code/bitbucket/libraries"/>
    <property name="findbugs.home" location="C:/progs/findbugs"/>
    <property name="catalina.home" location="C:/progs/apache-tomcat"/>
    -->
</project>
```

### Compile projects
You may want to start by cleaning everyting up. Launch a bash shell in the rmsng root directory and run the clean command: 
`$ gradle clean`
Now you can go into each of the router, RMS and viewer directories to work with the projects.

- router

		$ ./gradlew clean war -p router/server  
		During development, you can add the -Pdev=true flag  
		$ ./gradlew clean war -p router/server -Pdev=true  

- rms

		$ ./gradlew clean war -p rms/server -Pdev=true  
		
- viewer

		$ ./gradlew clean war -p viewer -Pdev=true  
		
If there are no problems, the three commands above should produce three WAR files that can be deployed in the RMS, router and viewer docker containers. 

### Initialize PostgreSQL

- Create **router** database

		$ sudo -u postgres psql postgres
		postgres=# CREATE USER router PASSWORD '123next!';
		postgres=# CREATE DATABASE router OWNER router ENCODING 'UTF-8';
		postgres=# \connect router;
		postgres=# SET ROLE router;
		postgres=# CREATE SCHEMA router AUTHORIZATION router;
		postgres=# \q

- Create **rms** database

		$ sudo -u postgres psql postgres
		postgres=# CREATE USER rms PASSWORD '123next!';
		postgres=# CREATE DATABASE rms OWNER rms ENCODING 'UTF-8';
		postgres=# \connect rms;
		postgres=# SET ROLE rms;
		postgres=# CREATE SCHEMA rms AUTHORIZATION rms;
		postgres=# \q

### Config tomcat on Windows
1. Config server.xml, you can copy sample server.xml to tomcat and change it as you want

		$ cp rms/server/conf/server.xml $CATALINA_HOME/conf

2. Config server certificate

		$ cp rms/server/conf/keystore.pfx $CATALINA_HOME/conf

3. Config logging. Merge rms, router and viewer logging.properties if you deploy them together.

		$ cp rms/server/conf/logging.properties $CATALINA_HOME/conf/logging.properties

4. Copy system provided libraries to tomcat

		$ cd libraries
		$ cp libs/web/system/postgresql-9.4-1204-jdbc42.jar $CATALINA_HOME/lib

5. Config administrative properties file, you might need merge rms and router properties if you deploy them together

		$ cp rms/server/conf/admin.properties $CATALINA_HOME/conf

6. Copy utilities shell script files to tomcat, this is optional

		$ cp router/server/conf/router.sh $CATALINA_HOME/conf

7. Start tomcat

		$ startup.sh

### Config tomcat on Centos
1. Install JDK 8 (if JDK 8 already present, skip this step)

		$ cd ~
		$ wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.rpm"

		$ sudo yum localinstall jdk-8u131-linux-x64.rpm
		$ rm ~/jdk-8u131-linux-x64.rpm

2. Create tomcat user

		$ sudo groupadd tomcat
		$ sudo useradd -M -s /bin/nologin -g tomcat -d /opt/tomcat tomcat

3. Install tomcat

		$ cd /tmp
		$ wget http://www-us.apache.org/dist/tomcat/tomcat-8/v8.5.16/bin/apache-tomcat-8.5.16.tar.gz
		$ tar -xvf /tmp/apache-tomcat-8.5.16.tar.gz
		$ mv apache-tomcat-8.5.16 /opt/tomcat
		$ chown -R tomcat:tomcat /opt/tomcat
		
		# create tomcat service and save it
		$ sudo nano /etc/systemd/system/tomcat.service
		
			# Systemd unit file for tomcat
			[Unit]
			Description=Apache Tomcat Web Application Container
			After=syslog.target network.target
			
			[Service]
			Type=forking
			
			Environment=JAVA_HOME=/opt/jdk1.8.0_101
			Environment=JRE_HOME=/opt/jdk1.8.0_101/jre
			Environment=CATALINA_PID=/opt/tomcat/temp/tomcat.pid
			Environment=CATALINA_HOME=/opt/tomcat
			Environment=CATALINA_BASE=/opt/tomcat
			Environment='JAVA_OPTS=-Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom'
			Environment='AUTHBIND_COMMAND="/usr/bin/authbind --deep /bin/bash -c "'
			
			ExecStart=/usr/bin/authbind --deep /bin/bash -c /opt/tomcat/bin/startup.sh
			ExecStop=/bin/kill -15 $MAINPID
			
			User=tomcat
			Group=tomcat
			
			[Install]
			WantedBy=multi-user.target
		
		$ sudo systemctl daemon-reload 
		$ sudo systemctl start tomcat 
		$ sudo systemctl enable tomcat
					

4. Config server.xml, copy sample server.xml to tomcat

		$ sudo cp rms/server/conf/server.xml $CATALINA_HOME/conf

5. Config server certificate

		$ sudo cp rms/server/conf/keystore.pfx $CATALINA_HOME/conf

6. Config logging. Merge rms, router and viewer logging.properties if you deploy them together.

		$ cp rms/server/conf/logging-tomcat7.properties $CATALINA_HOME/conf/logging.properties

7. Copy system provided libraries to tomcat

		$ cd libraries
		$ sudo cp libs/web/system/postgresql-9.4-1204-jdbc42.jar $CATALINA_HOME/lib

6. Create nextlabs conf folder

		$ sudo mkdir -p /var/opt/nextlabs/rms/shared/conf

7. Config administrative properties file, you might need merge rms and router properties if you deploy them together

		$ sudo cp rms/server/conf/admin.properties /var/opt/nextlabs/rms/shared/conf

8. Copy utilities shell script files to tomcat, this is optional

		$ sudo cp router/server/conf/router.sh /var/opt/nextlabs/rms/shared/conf

9. Start tomcat

		$ sudo service tomcat start

### Deploy war file to tomcat
```
$ cd rms/server
$ ./gradlew clean war

$ cd router/server
$ ./gradlew clean war
```

### Add tenant 

Add tenant to router by running the following command

```
./router.sh --cmd=Tenant --operation=add --name=skydrm.com --server=https://www.skydrm.com/rms

2016-06-13 11:23:50:129:  Tenant: nextlabs.com created, OTP:4864563C0259A1BC62B4891FEB45D070
```

Use the otp from above result to register tenant by invoking the rest API at https://bitbucket.org/nxtlbs-devops/rightsmanagement-wiki/wiki/RMS/RESTful%20API/Tenant%20REST%20API#markdown-header-register-a-new-tenant-on-rms

The otp for a tenant can also be reset using ```router.sh``` by setting ```--operation=reset-otp```.  Here is description of the options which can be used with ```router.sh```:

```
usage: java com.nextlabs.router.Main --cmd=Tenant
-d,--displayName <arg>      Tenant display name
-desc,--description <arg>   Tenant description
-e,--email <arg>            Contact email address
-n,--name <arg>             Tenant name
-o,--operation <arg>        Operation: add/reset-otp
-s,--server <arg>           Server name
```



### Deploy Viewer

1. Update RMS_URL, VIEWER_DATA_DIR and VIEWER_INSTALL_DIR properties in viewer/conf/viewer.properties. (OS user running the Tomcat process must have write permissions on these directories)
2. Copy updated viewer.properties to $CATALINA_HOME/conf. Copy viewer/conf/logging.properties to $CATALINA_HOME/conf. If you are deploying viewer along with rms or router, merge logging.properties

		$ sudo cp viewer/conf/viewer.properties /var/opt/nextlabs/rms/shared/conf

3. Update the web.viewer_url in {tomcat}/conf/admin.properties.
		
4. Create a folder license in <VIEWER_DATA_DIR> and copy libraries/license/license.jar into <VIEWER_DATA_DIR>/license/

5. Place license.dat (get it from peer developers) file along with license.jar 
6.  Create a folder viewers in <VIEWER_INSTALL_DIR> and copy the following zip files into it <VIEWER_INSTALL_DIR>/viewers from \\nextlabs.com\share\data\releases\SecureCollaboration\9.0.0.0-SAASOnly\4  

        RightsManagementServer-CADViewer-*.zip
        RightsManagementServer-DocViewer-*.zip
        RightsManagementServer-SAPViewer-*.zip

7. Add <VIEWER_INSTALL_DIR>/external/perceptive to PATH environment variable. (external/perceptive will be created when viewer packages are extracted). For LINUX set the following environment variables

```
#!shell

export LC_ALL="en_US.UTF-8" 
export DISPLAY=:0 
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:<VIEWER_INSTALL_DIR>/external/perceptive:<VIEWER_INSTALL_DIR>/external/RMSCADCONVERTER/bin/linux64
export ISYS_FONTS=<VIEWER_INSTALL_DIR>/external/perceptive/fonts
```
 

8. Build and deploy viewer.war 

        $ cd viewer
        $ ./gradlew clean war

 
9. Start the tomcat server. You can see the logs at VIEWER_DATA_DIR/logs/viewer.log. If you see the following statements in logs, then viewer packages have been deployed correctly and you should be able to launch the viewer

```
06-23-2016 15:16:30 INFO  LicenseManager: - Licensed Viewers: CAD Viewer, SAP 3D Viewer, Secure Viewer
06-23-2016 15:16:30 DEBUG ViewerInitializationManager: - Overwriting  with RightsManagementServer-CADViewer-8.3.0.0-295PS-main-201603310515.zip
06-23-2016 15:16:30 DEBUG ViewerInitializationManager: - Unzipping C:\NextLabs\RMS\INSTALL_DIR_NG\viewers\RightsManagementServer-CADViewer-8.3.0.0-295PS-main-201603310515.zip to C:\NextLabs\RMS\INSTALL_DIR_NG\external\RMSCADCONVERTER
06-23-2016 15:16:48 INFO  ViewerInitializationManager: - Deployed CAD Converter.
06-23-2016 15:16:49 INFO  ViewerInitializationManager: - Deployed CAD Viewer.
06-23-2016 15:16:49 DEBUG ViewerInitializationManager: - Overwriting  with RightsManagementServer-SAPViewer-8.3.0.0-14-201604250658.zip
06-23-2016 15:16:49 DEBUG ViewerInitializationManager: - Unzipping C:\NextLabs\RMS\INSTALL_DIR_NG\viewers\RightsManagementServer-SAPViewer-8.3.0.0-14-201604250658.zip to C:\Users\nnallagatla\apache-tomcat-8.0.35\webapps\viewer\ui\app\viewers
06-23-2016 15:17:18 INFO  ViewerInitializationManager: - Deployed SAP Viewer.
06-23-2016 15:17:18 DEBUG ViewerInitializationManager: - Overwriting  with RightsManagementServer-DocViewer-9.0.0.0-4-201605270119.zip
06-23-2016 15:17:18 DEBUG ViewerInitializationManager: - Unzipping C:\NextLabs\RMS\INSTALL_DIR_NG\viewers\RightsManagementServer-DocViewer-9.0.0.0-4-201605270119.zip to C:\NextLabs\RMS\INSTALL_DIR_NG\viewers\perceptive
06-23-2016 15:17:24 INFO  ViewerInitializationManager: - Deployed Doc Viewer.
```
#### Troubleshooting on windows
If you are unable to view CAD files, you should install:
```
https://www.microsoft.com/en-sg/download/details.aspx?id=40784 
https://www.microsoft.com/en-sg/download/details.aspx?id=30679 
```

#### Troubleshooting on linux

If you are unable to view CAD files, check if the following files are present


```
#Redhat/fedora/centos
/usr/lib64/libXext.so.6
/usr/lib64/libXmu.so.6
/usr/lib64/libGLU.so.1

#debian/ubuntu
/usr/lib/x86_64-linux-gnu/libXext.so.6
/usr/lib/x86_64-linux-gnu/libXmu.so.6
/usr/lib/x86_64-linux-gnu/libGLU.so.1
```

If above files are missing, run the following commands to install them

```
#Redhat/fedora/centos
yum install -y libXext-1.3.3-3.el7.x86_64 libXmu-1.1.2-2.el7.x86_64 mesa-libGLU-9.0.0-4.el7.x86_64

#debian/ubuntu
sudo apt-get install libXext6
sudo apt-get install libXmu6
sudo apt-get install libGLU1
```