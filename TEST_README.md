# Test Cases for Pharmacy Management System

This document outlines the comprehensive tests implemented for the Pharmacy Management System across backend unit tests, frontend unit/UI tests, and API tests.

## 1. JUnit (Unit Testing) - Backend Java

### Backend Service Test Files
- `AuthServiceTest`: login success, invalid password, role mismatch, blank role behavior, user-not-found, exception wrapping
- `MedicineServiceTest`: list medicines, add medicine success path, batch creation rules, insert failure path, rollback behavior
- `SalesServiceTest`: FEFO allocation success, insufficient stock rollback, batch update failure rollback
- `PurchaseServiceTest`: purchase success, inventory fallback create, vendor validation rollback, expiry validation rollback
- `ReorderServiceTest`: reorder report mapping, unknown medicine fallback, exception wrapping
- `ExpiredBatchDiscardServiceTest`: discard success report totals, discard failure rollback
- `ProfitReportServiceTest`: aggregate profit totals, medicine profit report success, missing medicine failure

### Current Backend Unit Test Count
- 7 test classes
- 25 passing tests

### Backend Detailed Test Review

#### `AuthServiceTest` (6 tests)
- Validates authentication success with matching username/password/role.
- Confirms password is removed (`null`) before user object is returned.
- Verifies login denial for invalid password.
- Verifies login denial for role mismatch and allows blank role input.
- Verifies `null` result when user does not exist.
- Verifies DAO/connection exceptions are wrapped as service-level runtime errors.

#### `MedicineServiceTest` (6 tests)
- Verifies medicine listing flow (`getAllMedicines`) returns DAO data.
- Validates full add flow: medicine insert, inventory creation, initial batch creation.
- Validates batch creation guard conditions:
  - no batch when initial quantity is `0`
  - no batch when vendor id is invalid (`<= 0`)
- Verifies behavior when medicine insert returns `false`.
- Verifies transactional rollback and wrapped exception when DAO throws.

#### `SalesServiceTest` (3 tests)
- Verifies successful sale end-to-end with FEFO batch allocation and transaction commit.
- Validates insufficient stock path triggers rollback.
- Validates batch reduction failure path triggers rollback.
- Confirms batch consumption and aggregate inventory deduction behavior.

#### `PurchaseServiceTest` (3 tests)
- Verifies successful purchase: vendor/medicine/mapping validation, header + details + batch insert, commit.
- Verifies inventory fallback path when `addQuantity` fails (creates inventory record).
- Validates rollback for vendor-not-found and invalid-expiry validation failures.

#### `ReorderServiceTest` (2 tests)
- Verifies reorder report generation from low-stock inventory records.
- Confirms medicine code fallback format (`UNKNOWN-{medicineId}`) when medicine is missing.
- Verifies recommended reorder quantity calculation (`threshold - current`).
- Verifies exception wrapping for connection/service failure path.

#### `ExpiredBatchDiscardServiceTest` (2 tests)
- Verifies successful expired-batch discard flow:
  - inventory reduction
  - batch quantity discard
  - report item generation
  - total discarded batches/units aggregation
- Verifies rollback and wrapped exception when batch discard update fails.

#### `ProfitReportServiceTest` (3 tests)
- Verifies full profit report totals (sales revenue, purchase cost, net profit).
- Verifies medicine and vendor breakdown DTO generation.
- Verifies medicine-specific profit report totals and profit margin inputs.
- Verifies missing medicine path throws wrapped service exception.

### Backend Test Design Notes
- Tests run as **unit tests with DAO stubs**, not DB integration tests.
- Service constructors were updated for dependency injection to avoid static/constructor mocking fragility.
- Transaction paths are explicitly tested for both commit and rollback scenarios.
- Coverage is intentionally balanced between:
  - happy paths
  - business rule validation failures
  - data mutation/transaction integrity failures

## 2. Selenium (Automated UI Testing) - Frontend

### UITests.test.js
- **should display login page**: Verifies login page loads correctly
- **should login as admin successfully**: Tests successful admin login flow
- **should show error for invalid login**: Tests error handling for invalid credentials
- **should navigate to medicine management**: Tests navigation to medicine management page
- **should add new medicine**: Tests the complete flow of adding a new medicine
- **should navigate to inventory management**: Tests navigation and search UI on the Inventory page
- **should navigate to vendor management**: Tests navigation and rendering of vendor management
- **should navigate to point of sale (Sales/POS)**: Tests navigation to the POS terminal
- **should open add vendor modal**: Tests opening the vendor registration modal
- **should verify POS sale elements**: Tests the presence of core POS UI controls (search map, checkout buttons)
- **should navigate to auto-generated orders**: Tests navigation to automated purchase orders and print functions

### Setup for Selenium Tests
Add to `frontend/package.json`:
```json
"devDependencies": {
  "selenium-webdriver": "^4.15.0",
  "chromedriver": "^119.0.0"
}
```

Run tests:
```bash
cd frontend
npm install
npm test selenium/UITests.test.js
```

## 3. Postman (API Testing) - Backend

### Pharmacy_API_Tests.postman_collection.json
- **Health Check**: Tests the health endpoint
- **User Login - Admin**: Tests successful admin login
- **User Login - Invalid Credentials**: Tests login with wrong credentials
- **Get All Medicines**: Tests fetching medicines list
- **Add Medicine**: Tests adding a new medicine
- **Get Sales**: Tests fetching sales data
- **Make Sale**: Tests creating a new sale
- **Get Profit Report**: Tests profit report generation

### Importing Postman Collection
1. Open Postman
2. Click "Import"
3. Select "File"
4. Choose `Pharmacy_API_Tests.postman_collection.json`
5. Set environment variable `base_url` to `http://localhost:8080`

## 4. Jest (Unit Testing) - Frontend React

### LoginView.test.js
- Renders login form with all required elements
- Submits form with correct data transformation (trim and lowercase username)
- Displays error messages
- Disables form during submission

### MedicineManagement.test.js
- Renders loading state initially
- Displays medicines list after data fetch
- Filters medicines based on search input
- Opens and submits add medicine form
- Handles successful medicine addition
- Handles errors during medicine addition

### authService.test.js
- Successfully authenticates user with valid credentials
- Handles 401 unauthorized responses
- Handles server errors
- Handles network errors
- Correctly maps backend user data

## Running All Tests

### Backend Unit Tests
```powershell
# from project root
powershell -ExecutionPolicy Bypass -File .\backend\run-tests.ps1
```

### Run One Backend Test Class
```powershell
# example: run only AuthService tests
$cp = "backend/lib/junit-4.13.2.jar;backend/lib/hamcrest-core-1.3.jar"
javac -cp $cp -d "backend/out" backend/src/com/msa/model/*.java backend/src/com/msa/dto/*.java backend/src/com/msa/dao/*.java backend/src/com/msa/db/*.java backend/src/com/msa/service/*.java backend/src/test/java/com/msa/*Test.java
java -cp "backend/out;$cp" org.junit.runner.JUnitCore com.msa.AuthServiceTest
```

### Backend Unit Tests (Manual Command)
```powershell
$cp = "backend/lib/junit-4.13.2.jar;backend/lib/hamcrest-core-1.3.jar"
javac -cp $cp -d "backend/out" backend/src/com/msa/model/*.java backend/src/com/msa/dto/*.java backend/src/com/msa/dao/*.java backend/src/com/msa/db/*.java backend/src/com/msa/service/*.java backend/src/test/java/com/msa/*Test.java
java -cp "backend/out;$cp" org.junit.runner.JUnitCore com.msa.AuthServiceTest com.msa.MedicineServiceTest com.msa.SalesServiceTest com.msa.PurchaseServiceTest com.msa.ReorderServiceTest com.msa.ExpiredBatchDiscardServiceTest com.msa.ProfitReportServiceTest
```

### Frontend Unit Tests
```bash
cd frontend
npm test
```

### Selenium UI Tests
```bash
cd frontend
npm run test:selenium
```

### Postman API Tests
Import collection and run in Postman with backend server running.

## Test Coverage Summary
- **JUnit**: Backend service layer unit tests (Auth, Medicine, Sales, Purchase, Reorder, ExpiredBatchDiscard, ProfitReport)
- **Selenium**: End-to-end UI automation tests
- **Postman**: API endpoint testing with automated assertions
- **Jest**: Frontend component and service unit tests

This suite now includes comprehensive backend service testing with transaction and failure-path coverage in addition to UI/API coverage.