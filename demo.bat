@echo off
cd /d "%~dp0"
set JAR=java -jar target\queuectl-1.0.0.jar

echo ====================================================
echo  QueueCTL End-to-End Demo
echo ====================================================
echo.

echo [1/7] Enqueueing jobs...
%JAR% enqueue {"id":"job1","command":"echo hello world"}
%JAR% enqueue {"id":"job2","command":"dir"}
%JAR% enqueue {"id":"job3","command":"echo high priority job","priority":10}
%JAR% enqueue {"id":"job4","command":"invalid_cmd_xyz","maxRetries":2}
%JAR% enqueue {"id":"job5","command":"echo final job"}
echo.

echo [2/7] Listing all jobs...
%JAR% list
echo.

echo [3/7] System status (before workers)...
%JAR% status
echo.

echo [4/7] Starting 3 workers (will process all jobs)...
echo       Workers will poll, execute, and exit when done.
echo.

REM Start workers in background, wait for them to finish
start /b java -jar target\queuectl-1.0.0.jar worker start --count 3

echo       Waiting 12 seconds for workers to process all jobs...
timeout /t 12 /nobreak >nul

echo.
echo [5/7] Listing jobs after processing...
%JAR% list
echo.

echo [6/7] System status (after workers)...
%JAR% status
echo.

echo [7/7] Dead Letter Queue...
%JAR% dlq list
echo.

echo ====================================================
echo  Demo complete!
echo ====================================================
