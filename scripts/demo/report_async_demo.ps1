$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$reportPayload = @{
    reportType = "TENANT_ACTIVITY"
    tenantId = 1
} | ConvertTo-Json

Write-Host "Enqueuing report job..."
$enqueue = Invoke-RestMethod -Method Post -Uri "$baseUrl/reports" -ContentType "application/json" -Body $reportPayload
$jobId = $enqueue.jobId
Write-Host "Queued job ID: $jobId"

if (-not $jobId) {
    throw "Could not extract jobId from enqueue response."
}

Write-Host "Polling job status until COMPLETED or FAILED..."
for ($i = 1; $i -le 20; $i++) {
    Start-Sleep -Seconds 1
    $status = Invoke-RestMethod -Method Get -Uri "$baseUrl/reports/$jobId/status"
    Write-Host ("Attempt {0}: status={1}" -f $i, $status.status)
    if ($status.status -eq "COMPLETED" -or $status.status -eq "FAILED") {
        $status | ConvertTo-Json -Depth 5
        break
    }
}
