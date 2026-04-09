# Test Cases for Pharmacy Management System

This document outlines the comprehensive test cases written for the Pharmacy Management System project, covering Unit Testing, Automated UI Testing, and API Testing.

## 1. JUnit (Unit Testing) - Backend Java

### MedicineServiceTest
- **getAllMedicines()**: Tests fetching all medicines from the database
- **addMedicine()**: Tests adding a new medicine with inventory and batch creation

### AuthServiceTest
- **login()**: Tests successful login with valid credentials
- **login()**: Tests login failure with invalid password
- **login()**: Tests login failure when user not found

## 2. Selenium (Automated UI Testing) - Frontend

### UITests.test.js
- **should display login page**: Verifies login page loads correctly
- **should login as admin successfully**: Tests successful admin login flow
- **should show error for invalid login**: Tests error handling for invalid credentials
- **should navigate to medicine management**: Tests navigation to medicine management page
- **should add new medicine**: Tests the complete flow of adding a new medicine

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
```bash
cd backend
javac -cp "lib/*" -d out src/test/java/com/msa/*.java
java -cp "out:lib/*" org.junit.runner.JUnitCore com.msa.MedicineServiceTest com.msa.AuthServiceTest
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
- **JUnit**: Backend service layer unit tests
- **Selenium**: End-to-end UI automation tests
- **Postman**: API endpoint testing with automated assertions
- **Jest**: Frontend component and service unit tests

This comprehensive test suite covers all major testing methodologies required for the project.