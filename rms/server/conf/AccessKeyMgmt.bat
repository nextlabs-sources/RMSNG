@echo off
rem ---------------------------------------------------------------------------
rem Example:
rem	accessKeyMgmt.bat --list
rem	accessKeyMgmt.bat --create --name=ABCD 
rem	accessKeyMgmt.bat --revoke --name=ABCD
rem	accessKeyMgmt.bat --refresh --name=ABCD
rem ---------------------------------------------------------------------------

setlocal

set "CURRENT_DIR=%cd%"
if "%RMS_INSTALL_DIR%" == "" (
	echo RMS_INSTALL_DIR is not set
	goto end
)

if not exist "%RMS_INSTALL_DIR%\external\tomcat" (
	echo Tomcat folder does not exist in %RMS_INSTALL_DIR%\external\tomcat
	goto end
)
set "CATALINA_HOME=%RMS_INSTALL_DIR%\external\tomcat"

if not exist "%RMS_INSTALL_DIR%\external\jre" (
	echo JRE folder does not exist in %RMS_INSTALL_DIR%\external\jre
	goto end
)
set "JAVA_HOME=%RMS_INSTALL_DIR%\external\jre\bin"

rem Guess CATALINA_HOME environment
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
echo The CATALINA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome


rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

call "%JAVA_HOME%\java" -cp "%CATALINA_HOME%\webapps\rms\WEB-INF\classes;%CATALINA_HOME%\webapps\rms\WEB-INF\lib\*;%CATALINA_HOME%\lib\*" com.nextlabs.rms.Main --cmd=AccessKeyMgmt %CMD_LINE_ARGS% 2> nul

:end
