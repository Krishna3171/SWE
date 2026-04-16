#!/usr/bin/env bash

set -euo pipefail

# Run with Bash, Git Bash, or WSL. Adjust the sample values below before using.

BASE_URL="${BASE_URL:-http://localhost:8080}"

ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
NEW_ADMIN_USERNAME="${NEW_ADMIN_USERNAME:-admin2}"
NEW_ADMIN_PASSWORD="${NEW_ADMIN_PASSWORD:-admin456}"

CASHIER_USERNAME="${CASHIER_USERNAME:-cashier1}"
CASHIER_PASSWORD="${CASHIER_PASSWORD:-cashier123}"
UPDATED_CASHIER_USERNAME="${UPDATED_CASHIER_USERNAME:-cashier2}"
UPDATED_CASHIER_PASSWORD="${UPDATED_CASHIER_PASSWORD:-cashier456}"

MEDICINE_CODE="${MEDICINE_CODE:-MED001}"
MEDICINE_ID="${MEDICINE_ID:-1}"
VENDOR_ID="${VENDOR_ID:-1}"
PURCHASE_ID="${PURCHASE_ID:-1}"

curl_get() {
  local path="$1"
  curl -sS "$BASE_URL${path}"
  printf '\n'
}

curl_json() {
  local method="$1"
  local path="$2"
  local payload="${3:-}"

  if [[ -n "$payload" ]]; then
    curl -sS -X "$method" "$BASE_URL${path}" \
      -H "Content-Type: application/json" \
      -d "$payload"
  else
    curl -sS -X "$method" "$BASE_URL${path}"
  fi

  printf '\n'
}

echo "== Auth =="
curl_json POST /api/users/login "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\",\"role\":\"admin\"}"

curl_json POST /api/users/admin/change-credentials "{\"adminUsername\":\"${ADMIN_USERNAME}\",\"adminPassword\":\"${ADMIN_PASSWORD}\",\"newUsername\":\"${NEW_ADMIN_USERNAME}\",\"newPassword\":\"${NEW_ADMIN_PASSWORD}\"}"

ADMIN_USERNAME="${NEW_ADMIN_USERNAME}"
ADMIN_PASSWORD="${NEW_ADMIN_PASSWORD}"

echo "== Cashiers =="
curl_json POST /api/users/admin/add-cashier "{\"adminUsername\":\"${ADMIN_USERNAME}\",\"adminPassword\":\"${ADMIN_PASSWORD}\",\"cashierUsername\":\"${CASHIER_USERNAME}\",\"cashierPassword\":\"${CASHIER_PASSWORD}\"}"

curl_get "/api/users/cashiers?adminUsername=${ADMIN_USERNAME}&adminPassword=${ADMIN_PASSWORD}"

curl_json POST /api/users/admin/update-cashier "{\"adminUsername\":\"${ADMIN_USERNAME}\",\"adminPassword\":\"${ADMIN_PASSWORD}\",\"currentUsername\":\"${CASHIER_USERNAME}\",\"newUsername\":\"${UPDATED_CASHIER_USERNAME}\",\"newPassword\":\"${UPDATED_CASHIER_PASSWORD}\"}"

curl_json POST /api/users/admin/delete-cashier "{\"adminUsername\":\"${ADMIN_USERNAME}\",\"adminPassword\":\"${ADMIN_PASSWORD}\",\"cashierUsername\":\"${UPDATED_CASHIER_USERNAME}\"}"

echo "== Medicines =="
curl_get /api/medicines

curl_json POST /api/medicines "{\"tradeName\":\"Paracetamol 500\",\"genericName\":\"Paracetamol\",\"unitSellingPrice\":12.50,\"unitPurchasePrice\":8.00}"

curl_get "/api/medicines/${MEDICINE_CODE}"

curl_json PUT "/api/medicines/${MEDICINE_CODE}" "{\"tradeName\":\"Paracetamol 650\",\"genericName\":\"Paracetamol\",\"unitSellingPrice\":14.00,\"unitPurchasePrice\":9.00}"

echo "== Vendors =="
curl_get /api/vendors

curl_json POST /api/vendors "{\"vendorName\":\"City Pharma Supplies\",\"address\":\"Main Street\",\"contactNo\":\"5551234567\"}"

curl_json PUT "/api/vendors/${VENDOR_ID}" "{\"vendorName\":\"City Pharma Supplies Ltd\",\"address\":\"Main Street\",\"contactNo\":\"5551234567\"}"

curl_get "/api/vendors/${VENDOR_ID}/medicines"

echo "== Vendor-Medicine Mapping =="
curl_get /api/vendor-medicine

curl_json POST /api/vendor-medicine "{\"adminUsername\":\"${ADMIN_USERNAME}\",\"adminPassword\":\"${ADMIN_PASSWORD}\",\"vendorId\":${VENDOR_ID},\"medicineId\":${MEDICINE_ID}}"

echo "== Inventory =="
curl_get /api/inventory

curl_get /api/inventory/low-stock

curl_json PUT "/api/inventory/${MEDICINE_ID}/threshold" "{\"reorderThreshold\":25}"

echo "== Purchases =="
curl_json POST /api/purchases/create "{\"vendorId\":${VENDOR_ID},\"items\":[{\"medicineCode\":\"${MEDICINE_CODE}\",\"batchNumber\":\"BATCH-001\",\"expiryDate\":\"2026-12-31\",\"quantity\":50,\"unitPurchasePrice\":8.00}]}"

curl_get /api/purchases

curl_get "/api/purchases/${PURCHASE_ID}"

curl_json POST /api/purchases/receive "{\"purchaseId\":${PURCHASE_ID}}"

echo "== Sales =="
curl_json POST /api/sales/create "{\"items\":[{\"medicineCode\":\"${MEDICINE_CODE}\",\"quantity\":2}]}"

echo "== Reports =="
curl_json POST /api/reports/profit "{\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}"

curl_json POST /api/reports/profit/medicine "{\"medicineId\":${MEDICINE_ID}}"

curl_json POST /api/reports/expired/discard "{}"

curl_get /api/reports/reorder

# Optional cleanup examples.
# Run these only after removing any dependent purchases, sales, and mappings.
# curl_json DELETE "/api/medicines/${MEDICINE_CODE}"
# curl_json DELETE "/api/vendors/${VENDOR_ID}"
# curl_json DELETE /api/vendor-medicine "{\"adminUsername\":\"${ADMIN_USERNAME}\",\"adminPassword\":\"${ADMIN_PASSWORD}\",\"vendorId\":${VENDOR_ID},\"medicineId\":${MEDICINE_ID}}"