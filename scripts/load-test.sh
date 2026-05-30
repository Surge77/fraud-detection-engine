#!/usr/bin/env bash
# Load test: fire N transactions across a set of accounts at the ingestion API
# and report p50/p95/p99 latency. The POST is async (returns 202), so this
# measures API ingestion latency — the path that must stay fast under load.
#
# Usage: ./scripts/load-test.sh [BASE_URL] [REQUESTS]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
REQUESTS="${2:-1000}"
P99_BUDGET_MS=200
ACCOUNTS=(acc_001 acc_002 acc_003 acc_004 acc_005)
MERCHANTS=(merch_001 merch_999 merch_002 merch_777 merch_003)
LOCATIONS=(Nigeria "United States" Belarus "United Kingdom" Iran)

timings_file="$(mktemp)"
trap 'rm -f "$timings_file"' EXIT

echo "Firing $REQUESTS transactions at $BASE_URL/api/v1/transactions ..."
for ((i = 0; i < REQUESTS; i++)); do
    account="${ACCOUNTS[$((i % ${#ACCOUNTS[@]}))]}"
    merchant="${MERCHANTS[$((i % ${#MERCHANTS[@]}))]}"
    location="${LOCATIONS[$((i % ${#LOCATIONS[@]}))]}"
    amount=$(( (RANDOM % 100000) + 1 ))
    body=$(cat <<JSON
{"transactionId":"$(uuidgen 2>/dev/null || echo "tx-$i-$RANDOM")","accountId":"$account","amount":$amount.00,"currency":"USD","merchantId":"$merchant","merchantName":"LoadTest","location":"$location","timestamp":"$(date -u +%Y-%m-%dT%H:%M:%SZ)"}
JSON
)
    # time_total is seconds with millisecond precision
    t=$(curl -s -o /dev/null -w '%{time_total}' -X POST "$BASE_URL/api/v1/transactions" \
        -H 'Content-Type: application/json' -d "$body")
    echo "$t * 1000" | bc -l >> "$timings_file"
done

sort -n "$timings_file" -o "$timings_file"
percentile() { # $1 = percentile (e.g. 99)
    awk -v p="$1" '{a[NR]=$0} END {idx=int((p/100)*NR); if(idx<1)idx=1; printf "%.1f", a[idx]}' "$timings_file"
}

p50=$(percentile 50); p95=$(percentile 95); p99=$(percentile 99)
echo "----------------------------------------"
printf "p50: %s ms\np95: %s ms\np99: %s ms\n" "$p50" "$p95" "$p99"
echo "----------------------------------------"

if (( $(echo "$p99 < $P99_BUDGET_MS" | bc -l) )); then
    echo "PASS: p99 ($p99 ms) under ${P99_BUDGET_MS}ms budget"
else
    echo "FAIL: p99 ($p99 ms) exceeds ${P99_BUDGET_MS}ms budget"
    exit 1
fi
