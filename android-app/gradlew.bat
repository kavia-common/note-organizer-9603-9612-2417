@rem
@rem Gradle wrapper script for Windows.
@rem This bootstraps Gradle using gradle\wrapper\gradle-wrapper.jar and the
@rem distributionUrl defined in gradle\wrapper\gradle-wrapper.properties.
@rem

@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0

set WRAPPER_JAR=%DIRNAME%gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
  echo ERROR: Gradle wrapper JAR not found at: "%WRAPPER_JAR%"
  echo Make sure gradle\wrapper\gradle-wrapper.jar is committed to the repository.
  exit /b 1
)

if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java.exe
)

"%JAVACMD%" "-Xmx64m" "-Xms64m" "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
