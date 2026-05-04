#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "Enqueuing report job..."
enqueue_response=$(curl -sS -X POST "${BASE_URL}/reports" \
  -H "Content-Type: application/json" \
  -d '{"reportType":"TENANT_ACTIVITY","tenantId":1}')
echo "Enqueue response: ${enqueue_response}"

job_id=$(printf '%s' "${enqueue_response}" | sed -n 's/.*"jobId":[[:space:]]*\([0-9][0-9]*\).*/\1/p')
if [[ -z "${job_id}" ]]; then
  echo "Could not parse jobId from response."
  exit 1
fi

echo "Polling job ${job_id} until COMPLETED or FAILED..."
for attempt in $(seq 1 20); do
  sleep 1
  status_response=$(curl -sS "${BASE_URL}/reports/${job_id}/status")
  status=$(printf '%s' "${status_response}" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
  echo "Attempt ${attempt}: status=${status}"

  if [[ "${status}" == "COMPLETED" || "${status}" == "FAILED" ]]; then
    echo "Final payload: ${status_response}"
    break
  fi
done
