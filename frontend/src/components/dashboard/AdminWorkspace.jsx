import { useState } from "react";
import SectionTabs from "./SectionTabs";
import Field from "../common/FormField";
import InlineFeedback from "../common/InlineFeedback";
import DataCards from "./DataCards";
import { deleteJson, getJson, postJson, putJson } from "../../services/apiClient";

const ADMIN_SECTIONS = [
  { id: "users", label: "Users" },
  { id: "catalog", label: "Catalog" },
  { id: "inventory", label: "Inventory" },
  { id: "reports", label: "Reports" },
];

const parseInteger = (value, label) => {
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) {
    throw new Error(`${label} must be a valid number`);
  }
  return parsed;
};

const parseDecimal = (value, label) => {
  const parsed = Number.parseFloat(value);
  if (Number.isNaN(parsed)) {
    throw new Error(`${label} must be a valid number`);
  }
  return parsed;
};

const normalize = (value) => value.trim().toLowerCase();

function AdminWorkspace({ user }) {
  const [activeSection, setActiveSection] = useState("users");
  const [isBusy, setIsBusy] = useState(false);
  const [feedback, setFeedback] = useState({ type: "", message: "" });
  const [fieldErrors, setFieldErrors] = useState({});

  const [adminForm, setAdminForm] = useState({
    adminUsername: user.username,
    adminPassword: "",
    newUsername: user.username,
    newPassword: "",
  });
  const [cashierForm, setCashierForm] = useState({
    adminUsername: user.username,
    adminPassword: "",
    cashierUsername: "cashier1",
    cashierPassword: "cashier123",
  });
  const [cashierUpdateForm, setCashierUpdateForm] = useState({
    adminUsername: user.username,
    adminPassword: "",
    cashierUsername: "cashier1",
    newUsername: "cashier2",
    newPassword: "cashier456",
  });
  const [medicineForm, setMedicineForm] = useState({
    medicineCode: "MED001",
    tradeName: "Paracetamol 500",
    genericName: "Paracetamol",
    unitSellingPrice: "12.5",
    unitPurchasePrice: "8.0",
  });
  const [vendorForm, setVendorForm] = useState({
    vendorId: "1",
    vendorName: "City Pharma Supplies",
    address: "Main Street",
    contactNo: "5551234567",
  });
  const [mappingForm, setMappingForm] = useState({
    adminUsername: user.username,
    adminPassword: "",
    vendorId: "1",
    medicineId: "1",
  });
  const [inventoryForm, setInventoryForm] = useState({ medicineId: "1", reorderThreshold: "20" });
  const [reportForm, setReportForm] = useState({
    startDate: "2026-01-01",
    endDate: "2026-12-31",
    medicineId: "1",
  });

  const [cashiers, setCashiers] = useState([]);
  const [inventoryRows, setInventoryRows] = useState([]);
  const [lowStockRows, setLowStockRows] = useState([]);
  const [reorderItems, setReorderItems] = useState([]);
  const [medicineProfits, setMedicineProfits] = useState([]);
  const [vendorProfits, setVendorProfits] = useState([]);

  const setField = (setter, field, value, formKey) => {
    setter((prev) => ({ ...prev, [field]: value }));
    if (formKey) {
      setFieldErrors((prev) => {
        const next = { ...(prev[formKey] || {}) };
        delete next[field];
        return { ...prev, [formKey]: next };
      });
    }
  };

  const setErrors = (formKey, errors) => {
    setFieldErrors((prev) => ({ ...prev, [formKey]: errors }));
  };

  const clearErrors = (formKey) => setErrors(formKey, {});

  const getError = (formKey, field) => fieldErrors[formKey]?.[field] || "";

  const withAction = async (successMessage, action) => {
    setIsBusy(true);
    setFeedback({ type: "", message: "" });
    try {
      await action();
      setFeedback({ type: "success", message: successMessage });
    } catch (error) {
      setFeedback({ type: "error", message: error.message || "Request failed" });
    } finally {
      setIsBusy(false);
    }
  };

  const runValidatedAction = async (formKey, validator, successMessage, action) => {
    const errors = validator();
    if (Object.keys(errors).length > 0) {
      setErrors(formKey, errors);
      setFeedback({ type: "error", message: "Fix the highlighted fields and try again." });
      return;
    }

    clearErrors(formKey);
    await withAction(successMessage, action);
  };

  const validateAdminForm = () => {
    const errors = {};
    if (!adminForm.adminUsername.trim()) errors.adminUsername = "Admin username is required";
    if (!adminForm.adminPassword) errors.adminPassword = "Admin password is required";
    if (!adminForm.newUsername.trim()) errors.newUsername = "New username is required";
    if (!adminForm.newPassword) errors.newPassword = "New password is required";
    if (normalize(adminForm.adminUsername) === normalize(adminForm.newUsername)) {
      errors.newUsername = "New username must be different";
    }
    return errors;
  };

  const validateCashierCreateForm = () => {
    const errors = {};
    if (!cashierForm.adminUsername.trim()) errors.adminUsername = "Admin username is required";
    if (!cashierForm.adminPassword) errors.adminPassword = "Admin password is required";
    if (!cashierForm.cashierUsername.trim()) errors.cashierUsername = "Cashier username is required";
    if (!cashierForm.cashierPassword) errors.cashierPassword = "Cashier password is required";
    return errors;
  };

  const validateCashierLookupForm = () => {
    const errors = {};
    if (!cashierForm.adminUsername.trim()) errors.adminUsername = "Admin username is required";
    if (!cashierForm.adminPassword) errors.adminPassword = "Admin password is required";
    return errors;
  };

  const validateCashierUpdateForm = () => {
    const errors = {};
    if (!cashierUpdateForm.adminUsername.trim()) errors.adminUsername = "Admin username is required";
    if (!cashierUpdateForm.adminPassword) errors.adminPassword = "Admin password is required";
    if (!cashierUpdateForm.cashierUsername.trim()) errors.cashierUsername = "Current cashier username is required";
    if (!cashierUpdateForm.newUsername.trim()) errors.newUsername = "New username is required";
    if (!cashierUpdateForm.newPassword) errors.newPassword = "New password is required";
    if (normalize(cashierUpdateForm.cashierUsername) === normalize(cashierUpdateForm.newUsername)) {
      errors.newUsername = "New username must be different";
    }
    return errors;
  };

  const validateMedicineForm = (mode) => {
    const errors = {};
    if ((mode === "update" || mode === "delete") && !medicineForm.medicineCode.trim()) {
      errors.medicineCode = "Medicine code is required";
    }
    if (mode !== "delete" && !medicineForm.tradeName.trim()) errors.tradeName = "Trade name is required";
    if (mode !== "delete" && !medicineForm.genericName.trim()) errors.genericName = "Generic name is required";
    if (mode !== "delete" && Number.isNaN(Number.parseFloat(medicineForm.unitSellingPrice))) {
      errors.unitSellingPrice = "Unit selling price must be a valid number";
    }
    if (mode !== "delete" && Number.isNaN(Number.parseFloat(medicineForm.unitPurchasePrice))) {
      errors.unitPurchasePrice = "Unit purchase price must be a valid number";
    }
    return errors;
  };

  const validateVendorForm = (mode) => {
    const errors = {};
    if ((mode === "update" || mode === "delete") && !vendorForm.vendorId.trim()) errors.vendorId = "Vendor ID is required";
    if (mode !== "delete" && !vendorForm.vendorName.trim()) errors.vendorName = "Vendor name is required";
    if (mode !== "delete" && !vendorForm.address.trim()) errors.address = "Address is required";
    if (mode !== "delete" && !vendorForm.contactNo.trim()) errors.contactNo = "Contact number is required";
    return errors;
  };

  const validateMappingForm = () => {
    const errors = {};
    if (!mappingForm.adminUsername.trim()) errors.adminUsername = "Admin username is required";
    if (!mappingForm.adminPassword) errors.adminPassword = "Admin password is required";
    if (Number.isNaN(Number.parseInt(mappingForm.vendorId, 10))) errors.vendorId = "Vendor ID must be a valid number";
    if (Number.isNaN(Number.parseInt(mappingForm.medicineId, 10))) errors.medicineId = "Medicine ID must be a valid number";
    return errors;
  };

  const validateInventoryForm = () => {
    const errors = {};
    if (Number.isNaN(Number.parseInt(inventoryForm.medicineId, 10))) errors.medicineId = "Medicine ID must be a valid number";
    if (Number.isNaN(Number.parseInt(inventoryForm.reorderThreshold, 10))) {
      errors.reorderThreshold = "Reorder threshold must be a valid number";
    }
    return errors;
  };

  const validateReportDates = () => {
    const errors = {};
    if (!reportForm.startDate) errors.startDate = "Start date is required";
    if (!reportForm.endDate) errors.endDate = "End date is required";
    if (reportForm.startDate && reportForm.endDate && reportForm.startDate > reportForm.endDate) {
      errors.endDate = "End date must be on or after start date";
    }
    return errors;
  };

  const validateReportMedicine = () => {
    const errors = validateReportDates();
    if (Number.isNaN(Number.parseInt(reportForm.medicineId, 10))) errors.medicineId = "Medicine ID must be a valid number";
    return errors;
  };

  const loadCashiers = () =>
    runValidatedAction("cashierLookup", validateCashierLookupForm, "Cashier list loaded", async () => {
      const list = await getJson(
        `/api/users/cashiers?adminUsername=${encodeURIComponent(cashierForm.adminUsername.trim())}&adminPassword=${encodeURIComponent(cashierForm.adminPassword)}`,
      );
      setCashiers(Array.isArray(list) ? list : []);
    });

  const loadInventory = () =>
    withAction("Inventory loaded", async () => {
      const list = await getJson("/api/inventory");
      setInventoryRows(Array.isArray(list) ? list : []);
    });

  const loadLowStock = () =>
    withAction("Low stock loaded", async () => {
      const list = await getJson("/api/inventory/low-stock");
      setLowStockRows(Array.isArray(list) ? list : []);
    });

  const loadReorder = () =>
    withAction("Reorder report loaded", async () => {
      const data = await getJson("/api/reports/reorder");
      setReorderItems(Array.isArray(data?.reorderItems) ? data.reorderItems : []);
    });

  return (
    <>
      <SectionTabs sections={ADMIN_SECTIONS} activeSection={activeSection} onSelect={setActiveSection} />
      <InlineFeedback feedback={feedback} />

      {activeSection === "users" && (
        <section className="panel-grid two-col">
          <article className="action-card form-card">
            <h3>Change admin credentials</h3>
            <div className="field-grid">
              <Field label="Admin username" error={getError("admin", "adminUsername")}>
                <input value={adminForm.adminUsername} onChange={(e) => setField(setAdminForm, "adminUsername", e.target.value, "admin")} />
              </Field>
              <Field label="Admin password" error={getError("admin", "adminPassword")}>
                <input type="password" value={adminForm.adminPassword} onChange={(e) => setField(setAdminForm, "adminPassword", e.target.value, "admin")} />
              </Field>
              <Field label="New username" error={getError("admin", "newUsername")}>
                <input value={adminForm.newUsername} onChange={(e) => setField(setAdminForm, "newUsername", e.target.value, "admin")} />
              </Field>
              <Field label="New password" error={getError("admin", "newPassword")}>
                <input type="password" value={adminForm.newPassword} onChange={(e) => setField(setAdminForm, "newPassword", e.target.value, "admin")} />
              </Field>
            </div>
            <button
              type="button"
              disabled={isBusy}
              onClick={() =>
                runValidatedAction("admin", validateAdminForm, "Admin credentials updated", () =>
                  postJson("/api/users/admin/change-credentials", adminForm),
                )
              }
            >
              Update admin
            </button>
          </article>

          <article className="action-card form-card">
            <h3>Cashier management</h3>
            <div className="field-grid">
              <Field label="Admin username" error={getError("cashier", "adminUsername")}>
                <input value={cashierForm.adminUsername} onChange={(e) => setField(setCashierForm, "adminUsername", e.target.value, "cashier")} />
              </Field>
              <Field label="Admin password" error={getError("cashier", "adminPassword")}>
                <input type="password" value={cashierForm.adminPassword} onChange={(e) => setField(setCashierForm, "adminPassword", e.target.value, "cashier")} />
              </Field>
              <Field label="Cashier username" error={getError("cashier", "cashierUsername")}>
                <input value={cashierForm.cashierUsername} onChange={(e) => setField(setCashierForm, "cashierUsername", e.target.value, "cashier")} />
              </Field>
              <Field label="Cashier password" error={getError("cashier", "cashierPassword")}>
                <input type="password" value={cashierForm.cashierPassword} onChange={(e) => setField(setCashierForm, "cashierPassword", e.target.value, "cashier")} />
              </Field>
            </div>
            <div className="button-row">
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("cashier", validateCashierCreateForm, "Cashier added", () =>
                    postJson("/api/users/admin/add-cashier", cashierForm),
                  )
                }
              >
                Add cashier
              </button>
              <button type="button" disabled={isBusy} onClick={loadCashiers}>
                Load cashiers
              </button>
            </div>
          </article>

          <article className="action-card form-card">
            <h3>Update or delete cashier</h3>
            <div className="field-grid">
              <Field label="Admin username" error={getError("cashierUpdate", "adminUsername")}>
                <input value={cashierUpdateForm.adminUsername} onChange={(e) => setField(setCashierUpdateForm, "adminUsername", e.target.value, "cashierUpdate")} />
              </Field>
              <Field label="Admin password" error={getError("cashierUpdate", "adminPassword")}>
                <input type="password" value={cashierUpdateForm.adminPassword} onChange={(e) => setField(setCashierUpdateForm, "adminPassword", e.target.value, "cashierUpdate")} />
              </Field>
              <Field label="Current cashier username" error={getError("cashierUpdate", "cashierUsername")}>
                <input value={cashierUpdateForm.cashierUsername} onChange={(e) => setField(setCashierUpdateForm, "cashierUsername", e.target.value, "cashierUpdate")} />
              </Field>
              <Field label="New username" error={getError("cashierUpdate", "newUsername")}>
                <input value={cashierUpdateForm.newUsername} onChange={(e) => setField(setCashierUpdateForm, "newUsername", e.target.value, "cashierUpdate")} />
              </Field>
              <Field label="New password" error={getError("cashierUpdate", "newPassword")}>
                <input type="password" value={cashierUpdateForm.newPassword} onChange={(e) => setField(setCashierUpdateForm, "newPassword", e.target.value, "cashierUpdate")} />
              </Field>
            </div>
            <div className="button-row">
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("cashierUpdate", validateCashierUpdateForm, "Cashier updated", () =>
                    postJson("/api/users/admin/update-cashier", cashierUpdateForm),
                  )
                }
              >
                Update cashier
              </button>
              <button
                type="button"
                className="danger"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("cashierUpdate", validateCashierUpdateForm, "Cashier deleted", () =>
                    postJson("/api/users/admin/delete-cashier", {
                      adminUsername: cashierUpdateForm.adminUsername,
                      adminPassword: cashierUpdateForm.adminPassword,
                      cashierUsername: cashierUpdateForm.cashierUsername,
                    }),
                  )
                }
              >
                Delete cashier
              </button>
            </div>
          </article>

          <DataCards
            title="Cashiers"
            items={cashiers}
            fields={[
              { key: "userId", label: "User ID" },
              { key: "username", label: "Username" },
              { key: "role", label: "Role" },
            ]}
            emptyMessage="Load cashiers to display cards."
          />
        </section>
      )}

      {activeSection === "catalog" && (
        <section className="panel-grid two-col">
          <article className="action-card form-card">
            <h3>Medicines</h3>
            <div className="field-grid">
              <Field label="Medicine code" error={getError("medicine", "medicineCode")}>
                <input value={medicineForm.medicineCode} onChange={(e) => setField(setMedicineForm, "medicineCode", e.target.value, "medicine")} />
              </Field>
              <Field label="Trade name" error={getError("medicine", "tradeName")}>
                <input value={medicineForm.tradeName} onChange={(e) => setField(setMedicineForm, "tradeName", e.target.value, "medicine")} />
              </Field>
              <Field label="Generic name" error={getError("medicine", "genericName")}>
                <input value={medicineForm.genericName} onChange={(e) => setField(setMedicineForm, "genericName", e.target.value, "medicine")} />
              </Field>
              <Field label="Unit selling price" error={getError("medicine", "unitSellingPrice")}>
                <input value={medicineForm.unitSellingPrice} onChange={(e) => setField(setMedicineForm, "unitSellingPrice", e.target.value, "medicine")} />
              </Field>
              <Field label="Unit purchase price" error={getError("medicine", "unitPurchasePrice")}>
                <input value={medicineForm.unitPurchasePrice} onChange={(e) => setField(setMedicineForm, "unitPurchasePrice", e.target.value, "medicine")} />
              </Field>
            </div>
            <div className="button-row">
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("medicine", () => validateMedicineForm("create"), "Medicine created", () =>
                    postJson("/api/medicines", {
                      tradeName: medicineForm.tradeName.trim(),
                      genericName: medicineForm.genericName.trim(),
                      unitSellingPrice: parseDecimal(medicineForm.unitSellingPrice, "Unit selling price"),
                      unitPurchasePrice: parseDecimal(medicineForm.unitPurchasePrice, "Unit purchase price"),
                    }),
                  )
                }
              >
                Create
              </button>
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("medicine", () => validateMedicineForm("update"), "Medicine updated", () =>
                    putJson(`/api/medicines/${encodeURIComponent(medicineForm.medicineCode.trim())}`, {
                      tradeName: medicineForm.tradeName.trim(),
                      genericName: medicineForm.genericName.trim(),
                      unitSellingPrice: parseDecimal(medicineForm.unitSellingPrice, "Unit selling price"),
                      unitPurchasePrice: parseDecimal(medicineForm.unitPurchasePrice, "Unit purchase price"),
                    }),
                  )
                }
              >
                Update
              </button>
              <button
                type="button"
                className="danger"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("medicine", () => validateMedicineForm("delete"), "Medicine deleted", () =>
                    deleteJson(`/api/medicines/${encodeURIComponent(medicineForm.medicineCode.trim())}`),
                  )
                }
              >
                Delete
              </button>
            </div>
          </article>

          <article className="action-card form-card">
            <h3>Vendors and mappings</h3>
            <div className="field-grid">
              <Field label="Vendor ID" error={getError("vendor", "vendorId")}>
                <input value={vendorForm.vendorId} onChange={(e) => setField(setVendorForm, "vendorId", e.target.value, "vendor")} />
              </Field>
              <Field label="Vendor name" error={getError("vendor", "vendorName")}>
                <input value={vendorForm.vendorName} onChange={(e) => setField(setVendorForm, "vendorName", e.target.value, "vendor")} />
              </Field>
              <Field label="Address" error={getError("vendor", "address")}>
                <input value={vendorForm.address} onChange={(e) => setField(setVendorForm, "address", e.target.value, "vendor")} />
              </Field>
              <Field label="Contact number" error={getError("vendor", "contactNo")}>
                <input value={vendorForm.contactNo} onChange={(e) => setField(setVendorForm, "contactNo", e.target.value, "vendor")} />
              </Field>
            </div>
            <div className="button-row">
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("vendor", () => validateVendorForm("create"), "Vendor created", () =>
                    postJson("/api/vendors", {
                      vendorName: vendorForm.vendorName.trim(),
                      address: vendorForm.address.trim(),
                      contactNo: vendorForm.contactNo.trim(),
                    }),
                  )
                }
              >
                Create vendor
              </button>
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("vendor", () => validateVendorForm("update"), "Vendor updated", () =>
                    putJson(`/api/vendors/${parseInteger(vendorForm.vendorId, "Vendor ID")}`, {
                      vendorName: vendorForm.vendorName.trim(),
                      address: vendorForm.address.trim(),
                      contactNo: vendorForm.contactNo.trim(),
                    }),
                  )
                }
              >
                Update vendor
              </button>
              <button
                type="button"
                className="danger"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("vendor", () => validateVendorForm("delete"), "Vendor deleted", () =>
                    deleteJson(`/api/vendors/${parseInteger(vendorForm.vendorId, "Vendor ID")}`),
                  )
                }
              >
                Delete vendor
              </button>
            </div>

            <div className="field-grid compact">
              <Field label="Map vendor ID" error={getError("mapping", "vendorId")}>
                <input value={mappingForm.vendorId} onChange={(e) => setField(setMappingForm, "vendorId", e.target.value, "mapping")} />
              </Field>
              <Field label="Map medicine ID" error={getError("mapping", "medicineId")}>
                <input value={mappingForm.medicineId} onChange={(e) => setField(setMappingForm, "medicineId", e.target.value, "mapping")} />
              </Field>
              <Field label="Admin username" error={getError("mapping", "adminUsername")}>
                <input value={mappingForm.adminUsername} onChange={(e) => setField(setMappingForm, "adminUsername", e.target.value, "mapping")} />
              </Field>
              <Field label="Admin password" error={getError("mapping", "adminPassword")}>
                <input type="password" value={mappingForm.adminPassword} onChange={(e) => setField(setMappingForm, "adminPassword", e.target.value, "mapping")} />
              </Field>
            </div>
            <div className="button-row">
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("mapping", validateMappingForm, "Vendor-medicine mapping linked", () =>
                    postJson("/api/vendor-medicine", {
                      adminUsername: mappingForm.adminUsername,
                      adminPassword: mappingForm.adminPassword,
                      vendorId: parseInteger(mappingForm.vendorId, "Vendor ID"),
                      medicineId: parseInteger(mappingForm.medicineId, "Medicine ID"),
                    }),
                  )
                }
              >
                Link mapping
              </button>
              <button
                type="button"
                className="danger"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("mapping", validateMappingForm, "Vendor-medicine mapping removed", () =>
                    deleteJson("/api/vendor-medicine", {
                      adminUsername: mappingForm.adminUsername,
                      adminPassword: mappingForm.adminPassword,
                      vendorId: parseInteger(mappingForm.vendorId, "Vendor ID"),
                      medicineId: parseInteger(mappingForm.medicineId, "Medicine ID"),
                    }),
                  )
                }
              >
                Unlink mapping
              </button>
            </div>
          </article>
        </section>
      )}

      {activeSection === "inventory" && (
        <section className="panel-grid two-col">
          <article className="action-card form-card">
            <h3>Inventory actions</h3>
            <div className="field-grid compact">
              <Field label="Medicine ID" error={getError("inventory", "medicineId")}>
                <input value={inventoryForm.medicineId} onChange={(e) => setField(setInventoryForm, "medicineId", e.target.value, "inventory")} />
              </Field>
              <Field label="Reorder threshold" error={getError("inventory", "reorderThreshold")}>
                <input value={inventoryForm.reorderThreshold} onChange={(e) => setField(setInventoryForm, "reorderThreshold", e.target.value, "inventory")} />
              </Field>
            </div>
            <div className="button-row">
              <button type="button" disabled={isBusy} onClick={loadInventory}>
                Load inventory
              </button>
              <button type="button" disabled={isBusy} onClick={loadLowStock}>
                Load low stock
              </button>
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("inventory", validateInventoryForm, "Inventory threshold updated", () =>
                    putJson(`/api/inventory/${parseInteger(inventoryForm.medicineId, "Medicine ID")}/threshold`, {
                      reorderThreshold: parseInteger(inventoryForm.reorderThreshold, "Reorder threshold"),
                    }),
                  )
                }
              >
                Update threshold
              </button>
            </div>
          </article>

          <DataCards
            title="Inventory"
            items={inventoryRows}
            fields={[
              { key: "medicineId", label: "Medicine ID" },
              { key: "quantityAvailable", label: "Available" },
              { key: "reorderThreshold", label: "Threshold" },
            ]}
            emptyMessage="Load inventory to display cards."
          />

          <DataCards
            title="Low stock"
            items={lowStockRows}
            fields={[
              { key: "medicineId", label: "Medicine ID" },
              { key: "quantityAvailable", label: "Available" },
              { key: "reorderThreshold", label: "Threshold" },
            ]}
            emptyMessage="Load low stock to display cards."
          />
        </section>
      )}

      {activeSection === "reports" && (
        <section className="panel-grid two-col">
          <article className="action-card form-card">
            <h3>Report actions</h3>
            <div className="field-grid compact">
              <Field label="Start date" error={getError("report", "startDate")}>
                <input type="date" value={reportForm.startDate} onChange={(e) => setField(setReportForm, "startDate", e.target.value, "report")} />
              </Field>
              <Field label="End date" error={getError("report", "endDate")}>
                <input type="date" value={reportForm.endDate} onChange={(e) => setField(setReportForm, "endDate", e.target.value, "report")} />
              </Field>
              <Field label="Medicine ID" error={getError("report", "medicineId")}>
                <input value={reportForm.medicineId} onChange={(e) => setField(setReportForm, "medicineId", e.target.value, "report")} />
              </Field>
            </div>
            <div className="button-row">
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("report", validateReportDates, "Profit report loaded", async () => {
                    const report = await postJson("/api/reports/profit", {
                      startDate: reportForm.startDate,
                      endDate: reportForm.endDate,
                    });
                    setMedicineProfits(Array.isArray(report?.medicineProfits) ? report.medicineProfits : []);
                    setVendorProfits(Array.isArray(report?.vendorProfits) ? report.vendorProfits : []);
                  })
                }
              >
                Profit report
              </button>
              <button
                type="button"
                disabled={isBusy}
                onClick={() =>
                  runValidatedAction("report", validateReportMedicine, "Medicine profit loaded", async () => {
                    const report = await postJson("/api/reports/profit/medicine", {
                      medicineId: parseInteger(reportForm.medicineId, "Medicine ID"),
                    });
                    setMedicineProfits([report]);
                  })
                }
              >
                Medicine profit
              </button>
              <button type="button" disabled={isBusy} onClick={loadReorder}>
                Reorder report
              </button>
            </div>
          </article>

          <DataCards
            title="Medicine report cards"
            items={medicineProfits}
            fields={[
              { key: "medicineId", label: "Medicine ID" },
              { key: "medicineName", label: "Medicine" },
              { key: "totalSalesRevenue", label: "Revenue" },
              { key: "totalPurchaseCost", label: "Cost" },
              { key: "profit", label: "Profit" },
            ]}
            emptyMessage="Run a profit report to display medicine cards."
          />

          <DataCards
            title="Vendor report cards"
            items={vendorProfits}
            fields={[
              { key: "vendorId", label: "Vendor ID" },
              { key: "vendorName", label: "Vendor" },
              { key: "totalPurchaseCost", label: "Purchase cost" },
            ]}
            emptyMessage="Run a profit report to display vendor cards."
          />

          <DataCards
            title="Reorder cards"
            items={reorderItems}
            fields={[
              { key: "medicineCode", label: "Medicine" },
              { key: "currentStock", label: "Stock" },
              { key: "reorderThreshold", label: "Threshold" },
              { key: "recommendedOrderQty", label: "Recommended" },
              { key: "vendorIds", label: "Vendors" },
            ]}
            emptyMessage="Run reorder report to display reorder cards."
          />
        </section>
      )}
    </>
  );
}

export default AdminWorkspace;