@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "CURRENT_DIR=%~dp0%"
) else (
  set CURRENT_DIR=.\
)

rem Guess DOTCMS_HOME if not defined
if not "%DOTCMS_HOME%" == "" goto gotHome
set DOTCMS_HOME=%CURRENT_DIR%..
if exist "%DOTCMS_HOME%\bin\startup.bat" goto okHome
cd ..
set DOTCMS_HOME=%cd%
cd %CURRENT_DIR%
:gotHome
if exist "%DOTCMS_HOME%\bin\startup.bat" goto okHome
echo The DOTCMS_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

rem Get standard environment variables
if exist "%DOTCMS_HOME%\bin\setenv.bat" call "%DOTCMS_HOME%\bin\setenv.bat"

echo Using DOTCMS_HOME:   %DOTCMS_HOME%
echo Using JAVA_HOME:       %JAVA_HOME%

cd "%DOTCMS_HOME%\bin"
gradlew.bat undeployPlugins