@echo off
cd /d "%~dp0"
set JAR=target\queuectl-1.0.0.jar

echo ====================================================
echo   QueueCTL Worker Demo
echo ====================================================
echo.

echo Starting 2 workers...
start /b java -jar %JAR% worker start --count 2 >nul 2>&1

echo Waiting 6 seconds for jobs to process...
timeout /t 6 /nobreak >nul

echo.
echo Stopping workers...
java -jar %JAR% worker stop 2>nul
timeout /t 2 /nobreak >nul

echo.
echo ====================================================
echo   Results After Processing
echo ====================================================
echo.
echo --- Job List ---
java -jar %JAR% list 2>nul

echo.
echo --- System Status ---
java -jar %JAR% status 2>nul

echo.
echo --- Dead Letter Queue ---
java -jar %JAR% dlq list 2>nul

echo.
echo --- Retry dead job4 from DLQ ---
java -jar %JAR% dlq retry job4 2>nul

echo.
echo --- Jobs After DLQ Retry ---
java -jar %JAR% list 2>nul

echo.
echo ====================================================
echo   Demo Complete!
echo ====================================================
