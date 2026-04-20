# Pharmacy Management System

## Overview

The Pharmacy Management System is designed to streamline the operations of pharmacies, enabling efficient management of drug inventory, prescriptions, and billing processes. This project aims to provide a user-friendly interface for pharmacy staff to manage daily activities effectively.

## Features

- **Inventory Management**: Keep track of pharmaceutical products, including their stock levels, expiration dates, and prices.
- **Prescription Management**: Process and manage prescriptions from doctors, including patient information and medication details.
- **Billing System**: Generate invoices for customers, process payments, and maintain financial records.
- **User Authentication**: Secure login for pharmacy staff, ensuring access control and data security.
- **Reporting**: Generate reports on inventory levels, sales, and customer transactions for better decision-making.

## Architecture

The Pharmacy Management System follows a layered architecture:

1. **Presentation Layer**: The User Interface (UI) developed using [Frontend Technology] (e.g., React, Angular).
2. **Business Logic Layer**: Contains the core business logic and validation for the application.
3. **Data Access Layer**: Responsible for database operations, using [Database Technology] (e.g., MySQL, MongoDB).
4. **Database Layer**: Stores all data related to inventory, prescriptions, and users.

## Setup Instructions

### Prerequisites

- **Java JDK** (Version 17+ recommended)
- **Node.js** (Version 16+ recommended for React)
- **Git** (to clone the repo)

### 1. Clone the Repository

```bash
git clone https://github.com/Krishna3171/SWE.git
cd SWE
```

### 2. Configure Database

The backend currently connects to a remote Supabase PostgreSQL database automatically via `backend/src/com/msa/db/DBConnection.java`.
There is **no localized database setup required** to test this POC.

### 3. Start the Java Backend

Open a terminal inside the `/backend` folder.

**On Mac / Linux (Terminal / zsh / bash):**

```bash
cd backend
mkdir -p out
javac -cp "lib/*" -d out $(find src -name "*.java")
java -cp "out:lib/*" com.msa.main.Main
```

> **Note:** If you get a `java.net.BindException: Address already in use` error, it means the backend is already running. You can kill the old instance by running: `lsof -ti :8080 | xargs kill -9`

**On Windows (PowerShell):**

```powershell
cd backend
mkdir out -Force
javac -cp "lib/*" -d out (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp "out;lib/*" com.msa.main.Main
```

> **Note:** If you get a `java.net.BindException: Address already in use` error, you can kill the old instance by running: `Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force`

You should see: `MSA API server started on port 8080`. Leave this terminal open.

### 4. Start the React Frontend

Open a **new** separate terminal window inside the `/frontend` folder.

```bash
cd frontend
npm install    # Installs the dependencies (run this only the first time)
npm start      # Starts the React development server
```

The application will automatically open in your browser at `http://localhost:3000`.

## Usage Examples

- **Adding a New Product**:
  1.  Go to the "Inventory" section in the UI.
  2.  Click on "Add Product" and fill in the details.
  3.  Save the product to update the inventory.

- **Generating an Invoice**:
  1.  Go to the "Billing" section.
  2.  Select the products purchased and enter the customer details.
  3.  Click on "Generate Invoice" to complete the transaction.

## Conclusion

The Pharmacy Management System serves as a robust solution for pharmacy operations, making it easier to manage inventory, prescriptions, and billing efficiently. For contributions or inquiries, feel free to reach out to the project maintainer.
