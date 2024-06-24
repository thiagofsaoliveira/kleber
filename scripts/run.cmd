@echo off
setlocal

for %%f in (kleber*.jar) do (
    set "JAR_FILE=%%f"
    goto :found
)

echo No .jar file starting with 'kleber' found in the current directory.
exit /b 1

:found
echo Running %JAR_FILE%...
java -jar "%JAR_FILE%"
