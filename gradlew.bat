@echo off
setlocal
set APP_HOME=%~dp0
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java
)

"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
