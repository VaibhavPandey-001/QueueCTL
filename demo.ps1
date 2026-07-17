# QueueCTL End-to-End Demo
$ErrorActionPreference = "SilentlyContinue"
$jar = "C:\Users\vaibh\OneDrive\Desktop\FLAM\queuectl\target\queuectl-1.0.0.jar"

Write-Host ""
Write-Host "===================================================="
Write-Host "  QueueCTL End-to-End Demo"
Write-Host "===================================================="
Write-Host ""

# Enqueue jobs using a temp file approach for reliable JSON passing
Write-Host "[Step 1] Enqueueing 5 jobs..."
Write-Host ""

$json1 = '{"id":"job1","command":"echo hello world"}'
$json2 = '{"id":"job2","command":"dir"}'
$json3 = '{"id":"job3","command":"echo high priority job","priority":10}'
$json4 = '{"id":"job4","command":"invalid_cmd_xyz","maxRetries":2}'
$json5 = '{"id":"job5","command":"echo final job"}'

# Write each JSON to a temp file and pass it
$json1 | Out-File -Encoding utf8 -NoNewline "$env:TEMP\q1.json"
$json2 | Out-File -Encoding utf8 -NoNewline "$env:TEMP\q2.json"
$json3 | Out-File -Encoding utf8 -NoNewline "$env:TEMP\q3.json"
$json4 | Out-File -Encoding utf8 -NoNewline "$env:TEMP\q4.json"
$json5 | Out-File -Encoding utf8 -NoNewline "$env:TEMP\q5.json"

# Use cmd.exe to invoke jar (avoids PowerShell quote mangling)
& cmd /c "java -jar `"$jar`" enqueue %%" -replace '%%', "%%" 2>$null

# Actually let's just use a different approach - invoke via ProcessBuilder-style
$jobs = @(
    @{id="job1"; command="echo hello world"},
    @{id="job2"; command="dir"},
    @{id="job3"; command="echo high priority job"; priority=10},
    @{id="job4"; command="invalid_cmd_xyz"; maxRetries=2},
    @{id="job5"; command="echo final job"}
)

foreach ($j in $jobs) {
    $json = $j | ConvertTo-Json -Compress
    $process = Start-Process -FilePath "java" -ArgumentList "-jar", "`"$jar`"", "enqueue", $json -NoNewWindow -Wait -PassThru -RedirectStandardOutput "$env:TEMP\out.txt" -RedirectStandardError "$env:TEMP\err.txt"
    $output = Get-Content "$env:TEMP\err.txt" -ErrorAction SilentlyContinue
    if (-not $output) { $output = Get-Content "$env:TEMP\out.txt" -ErrorAction SilentlyContinue }
    Write-Host "  $($output)"
}

Write-Host ""
Write-Host "[Step 2] Listing all jobs..."
Write-Host ""
& java -jar $jar list 2>&1

Write-Host ""
Write-Host "[Step 3] Status before workers..."
Write-Host ""
& java -jar $jar status 2>&1

Write-Host ""
Write-Host "[Step 4] Starting workers in background (3 workers, 10s window)..."
Write-Host ""

# Start workers in a background job
$workerJob = Start-Job -ScriptBlock {
    param($jarPath)
    & java -jar $jarPath worker start --count 3
} -ArgumentList $jar

Start-Sleep -Seconds 10

Write-Host ""
Write-Host "[Step 5] Stopping workers..."
Write-Host ""
& java -jar $jar worker stop 2>&1

Start-Sleep -Seconds 2

# Kill the background worker job
Stop-Job -Job $workerJob -ErrorAction SilentlyContinue
Remove-Job -Job $workerJob -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "[Step 6] Listing jobs after processing..."
Write-Host ""
& java -jar $jar list 2>&1

Write-Host ""
Write-Host "[Step 7] System status after processing..."
Write-Host ""
& java -jar $jar status 2>&1

Write-Host ""
Write-Host "[Step 8] Dead Letter Queue..."
Write-Host ""
& java -jar $jar dlq list 2>&1

Write-Host ""
Write-Host "[Step 9] Retry a dead job (if any)..."
Write-Host ""
$dlqOutput = & java -jar $jar dlq list 2>&1
if ($dlqOutput -notmatch "No jobs") {
    & java -jar $jar dlq retry job4 2>&1
} else {
    Write-Host "  No dead jobs to retry."
}

Write-Host ""
Write-Host "[Step 10] Configuration..."
Write-Host ""
& java -jar $jar config get 2>&1

Write-Host ""
Write-Host "[Step 11] Set max-retries to 5..."
Write-Host ""
& java -jar $jar config set max-retries 5 2>&1
& java -jar $jar config get 2>&1

Write-Host ""
Write-Host "===================================================="
Write-Host "  Demo Complete!"
Write-Host "===================================================="
