import { useState, useEffect } from "react";
import SectionTabs from "./SectionTabs";
import Field from "../common/FormField";
import InlineFeedback from "../common/InlineFeedback";
import DataCards from "./DataCards";
import { getJson, postJson } from "../../services/apiClient";

const CASHIER_SECTIONS = [
  { id: "purchases", label: "Purchases" },
  { id: "sales", label: "Sales" },
];

function CashierWorkspace() {
  const [activeSection, setActiveSection] = useState("purchases");
  const [isBusy, setIsBusy] = useState(false);
  const [feedback, setFeedback] = useState({ type: "", message: "" });

  // Purchase flow state
  const [vendors, setVendors] = useState([]);
  const [selectedVendor, setSelectedVendor] = useState(null);
  const [vendorMedicines, setVendorMedicines] = useState([]);
  const [purchaseCart, setPurchaseCart] = useState([]);
  const [purchaseId, setPurchaseId] = useState("1");

  // Sales flow state
  const [allMedicines, setAllMedicines] = useState([]);
  const [saleCart, setSaleCart] = useState([]);
  const [saleQuantities, setSaleQuantities] = useState({}); // Track quantity inputs for each medicine

  // History state
  const [purchases, setPurchases] = useState([]);

  // Load vendors on mount
  useEffect(() => {
    const loadVendors = async () => {
      try {
        const data = await getJson("/api/vendors");
        setVendors(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error("Failed to load vendors", error);
      }
    };
    loadVendors();
  }, []);

  // Load all medicines for sales
  useEffect(() => {
    const loadMedicines = async () => {
      try {
        const data = await getJson("/api/medicines");
        setAllMedicines(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error("Failed to load medicines", error);
      }
    };
    loadMedicines();
  }, []);

  // Load vendor medicines when vendor changes
  useEffect(() => {
    const loadVendorMedicines = async () => {
      if (!selectedVendor) {
        setVendorMedicines([]);
        return;
      }
      try {
        const data = await getJson(`/api/vendors/${selectedVendor.vendorId}/medicines`);
        setVendorMedicines(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error("Failed to load vendor medicines", error);
      }
    };
    loadVendorMedicines();
  }, [selectedVendor]);

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

  // Purchase flow handlers
  const handleAddToPurchaseCart = (medicine) => {
    const existingItem = purchaseCart.find(
      (item) => item.medicineCode === medicine.medicineCode
    );

    if (existingItem) {
      setPurchaseCart(
        purchaseCart.map((item) =>
          item.medicineCode === medicine.medicineCode
            ? { ...item, quantity: Math.max(1, item.quantity + 1) }
            : item
        )
      );
    } else {
      setPurchaseCart([
        ...purchaseCart,
        {
          medicineCode: medicine.medicineCode,
          medicineName: medicine.medicineName,
          quantity: 1,
          unitPurchasePrice: medicine.unitPurchasePrice || 0,
          batchNumber: "BATCH001",
          expiryDate: "2026-12-31",
        },
      ]);
    }
  };

  const handleRemoveFromPurchaseCart = (medicineCode) => {
    setPurchaseCart(
      purchaseCart.filter((item) => item.medicineCode !== medicineCode)
    );
  };

  const handleUpdatePurchaseQuantity = (medicineCode, newQuantity) => {
    if (newQuantity < 1) {
      handleRemoveFromPurchaseCart(medicineCode);
      return;
    }
    setPurchaseCart(
      purchaseCart.map((item) =>
        item.medicineCode === medicineCode
          ? { ...item, quantity: newQuantity }
          : item
      )
    );
  };

  // Sales flow handlers
  const handleAddToSaleCart = (medicine, quantity) => {
    if (!quantity || quantity < 1) {
      setFeedback({ type: "error", message: "Enter a valid quantity" });
      return;
    }

    const existingItem = saleCart.find(
      (item) => item.medicineCode === medicine.medicineCode
    );

    if (existingItem) {
      setSaleCart(
        saleCart.map((item) =>
          item.medicineCode === medicine.medicineCode
            ? { ...item, quantity: item.quantity + quantity }
            : item
        )
      );
    } else {
      setSaleCart([
        ...saleCart,
        {
          medicineCode: medicine.medicineCode,
          medicineName: medicine.medicineName,
          quantity,
        },
      ]);
    }
    setFeedback({ type: "success", message: `${medicine.medicineName} added to cart` });
  };

  const handleRemoveFromSaleCart = (medicineCode) => {
    setSaleCart(saleCart.filter((item) => item.medicineCode !== medicineCode));
  };

  const handleUpdateSaleQuantity = (medicineCode, newQuantity) => {
    if (newQuantity < 1) {
      handleRemoveFromSaleCart(medicineCode);
      return;
    }
    setSaleCart(
      saleCart.map((item) =>
        item.medicineCode === medicineCode
          ? { ...item, quantity: newQuantity }
          : item
      )
    );
  };

  const purchaseTotalAmount = purchaseCart.reduce(
    (sum, item) => sum + item.quantity * item.unitPurchasePrice,
    0
  );

  const saleItemCount = saleCart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <>
      <SectionTabs sections={CASHIER_SECTIONS} activeSection={activeSection} onSelect={setActiveSection} />
      <InlineFeedback feedback={feedback} />

      {activeSection === "purchases" && (
        <section style={styles.mainContainer}>
          {/* Left side - Browse medicines */}
          <div style={styles.browsePanel}>
            <h3>📦 Select Vendor</h3>
            <div style={styles.vendorGrid}>
              {vendors.length === 0 ? (
                <p style={styles.emptyText}>No vendors available</p>
              ) : (
                vendors.map((vendor) => (
                  <button
                    key={vendor.vendorId}
                    onClick={() => setSelectedVendor(vendor)}
                    style={{
                      ...styles.vendorButton,
                      ...(selectedVendor?.vendorId === vendor.vendorId
                        ? styles.vendorButtonActive
                        : {}),
                    }}
                  >
                    <div style={styles.vendorButtonContent}>
                      <strong>{vendor.vendorName}</strong>
                      <small>{vendor.contactNumber}</small>
                    </div>
                  </button>
                ))
              )}
            </div>

            {selectedVendor && (
              <>
                <h3 style={styles.medicinesHeading}>
                  💊 {selectedVendor.vendorName}'s Medicines
                </h3>
                <div style={styles.medicineList}>
                  {vendorMedicines.length === 0 ? (
                    <p style={styles.emptyText}>No medicines from this vendor</p>
                  ) : (
                    vendorMedicines.map((medicine) => (
                      <div
                        key={medicine.medicineCode}
                        style={styles.medicineCard}
                      >
                        <div style={styles.medicineCardContent}>
                          <strong>{medicine.medicineName}</strong>
                          <small style={styles.medicineCode}>
                            Code: {medicine.medicineCode}
                          </small>
                          <div style={styles.priceTag}>
                            ₹{Number(medicine.unitPurchasePrice || 0).toFixed(2)}
                          </div>
                        </div>
                        <button
                          onClick={() => handleAddToPurchaseCart(medicine)}
                          style={styles.addButton}
                          disabled={isBusy}
                        >
                          + Add
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </>
            )}
          </div>

          {/* Right side - Cart */}
          <div style={styles.cartPanel}>
            <h3>🛒 Purchase Cart</h3>

            {purchaseCart.length === 0 ? (
              <p style={styles.emptyCartText}>Cart is empty. Select medicines to add.</p>
            ) : (
              <>
                <div style={styles.cartItemsList}>
                  {purchaseCart.map((item) => (
                    <div key={item.medicineCode} style={styles.cartItem}>
                      <div>
                        <strong>{item.medicineName}</strong>
                        <div style={styles.cartItemMeta}>
                          Code: {item.medicineCode}
                        </div>
                        <div style={styles.cartItemPrice}>
                          ₹{Number(item.unitPurchasePrice).toFixed(2)} each
                        </div>
                      </div>
                      <div style={styles.quantityControl}>
                        <button
                          onClick={() =>
                            handleUpdatePurchaseQuantity(
                              item.medicineCode,
                              item.quantity - 1
                            )
                          }
                          style={styles.qtyBtn}
                        >
                          −
                        </button>
                        <input
                          type="number"
                          min="1"
                          value={item.quantity}
                          onChange={(e) =>
                            handleUpdatePurchaseQuantity(
                              item.medicineCode,
                              parseInt(e.target.value, 10) || 1
                            )
                          }
                          style={styles.qtyInput}
                        />
                        <button
                          onClick={() =>
                            handleUpdatePurchaseQuantity(
                              item.medicineCode,
                              item.quantity + 1
                            )
                          }
                          style={styles.qtyBtn}
                        >
                          +
                        </button>
                        <button
                          onClick={() =>
                            handleRemoveFromPurchaseCart(item.medicineCode)
                          }
                          style={styles.removeBtn}
                        >
                          ✕
                        </button>
                      </div>
                      <div style={styles.cartItemTotal}>
                        ₹
                        {(item.quantity * item.unitPurchasePrice).toFixed(2)}
                      </div>
                    </div>
                  ))}
                </div>

                <div style={styles.cartSummary}>
                  <div style={styles.summaryRow}>
                    <span>Items:</span>
                    <strong>{purchaseCart.length}</strong>
                  </div>
                  <div style={styles.summaryRow}>
                    <span>Subtotal:</span>
                    <strong>₹{purchaseTotalAmount.toFixed(2)}</strong>
                  </div>
                  <div style={styles.divider} />
                  <div style={styles.summaryRow}>
                    <span>TOTAL:</span>
                    <strong style={styles.totalAmount}>
                      ₹{purchaseTotalAmount.toFixed(2)}
                    </strong>
                  </div>
                </div>

                <div style={styles.checkoutSection}>
                  <Field label="Purchase ID">
                    <input
                      type="number"
                      value={purchaseId}
                      onChange={(e) => setPurchaseId(e.target.value)}
                      disabled={isBusy}
                    />
                  </Field>
                  <button
                    onClick={() =>
                      withAction("Purchase created successfully!", () =>
                        postJson("/api/purchases/create", {
                          vendorId: selectedVendor.vendorId,
                          items: purchaseCart,
                        })
                      )
                    }
                    style={styles.checkoutBtn}
                    disabled={isBusy || purchaseCart.length === 0}
                  >
                    {isBusy ? "Processing..." : "✓ Complete Purchase"}
                  </button>
                </div>

                <div style={styles.actionsRow}>
                  <button
                    onClick={() =>
                      withAction("Purchases loaded", async () => {
                        const list = await getJson("/api/purchases");
                        setPurchases(Array.isArray(list) ? list : []);
                      })
                    }
                    style={styles.secondaryBtn}
                    disabled={isBusy}
                  >
                    Load Purchase History
                  </button>
                  <button
                    onClick={() => setPurchaseCart([])}
                    style={styles.secondaryBtn}
                    disabled={isBusy}
                  >
                    Clear Cart
                  </button>
                </div>
              </>
            )}
          </div>
        </section>
      )}

      {/* Purchase history cards */}
      {activeSection === "purchases" && purchases.length > 0 && (
        <section style={styles.historySection}>
          <DataCards
            title="📋 Purchase History"
            items={purchases}
            fields={[
              { key: "purchaseId", label: "Purchase ID" },
              { key: "vendorName", label: "Vendor" },
              { key: "totalAmount", label: "Total" },
              { key: "status", label: "Status" },
            ]}
            emptyMessage="No purchases yet."
          />
        </section>
      )}

      {/* SALES SECTION */}
      {activeSection === "sales" && (
        <section style={styles.salesContainer}>
          {/* Left side - Medicine selection */}
          <div style={styles.salesBrowsePanel}>
            <h3>💊 Available Medicines</h3>
            <div style={styles.medicineList}>
              {allMedicines.length === 0 ? (
                <p style={styles.emptyText}>No medicines available</p>
              ) : (
                allMedicines.map((medicine) => {
                  const quantity = saleQuantities[medicine.medicineCode] || 1;
                  return (
                    <div
                      key={medicine.medicineCode}
                      style={styles.medicineSelectionCard}
                    >
                      <div>
                        <strong>{medicine.medicineName}</strong>
                        <small style={styles.medicineCode}>
                          Code: {medicine.medicineCode}
                        </small>
                      </div>
                      <div style={styles.saleSelectionRow}>
                        <input
                          type="number"
                          min="1"
                          value={quantity}
                          onChange={(e) =>
                            setSaleQuantities({
                              ...saleQuantities,
                              [medicine.medicineCode]: parseInt(e.target.value, 10) || 1,
                            })
                          }
                          style={styles.saleQtyInput}
                          placeholder="Qty"
                        />
                        <button
                          onClick={() =>
                            handleAddToSaleCart(medicine, quantity)
                          }
                          style={styles.addButton}
                          disabled={isBusy}
                        >
                          Add to Cart
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Right side - Sales cart */}
          <div style={styles.saleCartPanel}>
            <h3>🛍️ Sale Cart</h3>

            {saleCart.length === 0 ? (
              <p style={styles.emptyCartText}>Cart is empty. Add medicines to sell.</p>
            ) : (
              <>
                <div style={styles.cartItemsList}>
                  {saleCart.map((item) => (
                    <div key={item.medicineCode} style={styles.cartItem}>
                      <div>
                        <strong>{item.medicineName}</strong>
                        <div style={styles.cartItemMeta}>
                          Code: {item.medicineCode}
                        </div>
                      </div>
                      <div style={styles.quantityControl}>
                        <button
                          onClick={() =>
                            handleUpdateSaleQuantity(
                              item.medicineCode,
                              item.quantity - 1
                            )
                          }
                          style={styles.qtyBtn}
                        >
                          −
                        </button>
                        <input
                          type="number"
                          min="1"
                          value={item.quantity}
                          onChange={(e) =>
                            handleUpdateSaleQuantity(
                              item.medicineCode,
                              parseInt(e.target.value, 10) || 1
                            )
                          }
                          style={styles.qtyInput}
                        />
                        <button
                          onClick={() =>
                            handleUpdateSaleQuantity(
                              item.medicineCode,
                              item.quantity + 1
                            )
                          }
                          style={styles.qtyBtn}
                        >
                          +
                        </button>
                        <button
                          onClick={() =>
                            handleRemoveFromSaleCart(item.medicineCode)
                          }
                          style={styles.removeBtn}
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                <div style={styles.cartSummary}>
                  <div style={styles.summaryRow}>
                    <span>Medicines:</span>
                    <strong>{saleCart.length}</strong>
                  </div>
                  <div style={styles.summaryRow}>
                    <span>Total Items:</span>
                    <strong>{saleItemCount}</strong>
                  </div>
                </div>

                <div style={styles.checkoutSection}>
                  <button
                    onClick={() =>
                      withAction("Sale completed successfully!", () =>
                        postJson("/api/sales/create", {
                          items: saleCart,
                        })
                      )
                    }
                    style={styles.checkoutBtn}
                    disabled={isBusy || saleCart.length === 0}
                  >
                    {isBusy ? "Processing..." : "✓ Submit Sale"}
                  </button>
                  <button
                    onClick={() => setSaleCart([])}
                    style={styles.secondaryBtn}
                    disabled={isBusy}
                  >
                    Clear Cart
                  </button>
                </div>
              </>
            )}
          </div>
        </section>
      )}
    </>
  );
}

const styles = {
  mainContainer: {
    display: "grid",
    gridTemplateColumns: "1fr 350px",
    gap: "20px",
    padding: "20px",
  },
  browsePanel: {
    display: "flex",
    flexDirection: "column",
    gap: "20px",
  },
  vendorGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(140px, 1fr))",
    gap: "10px",
  },
  vendorButton: {
    padding: "12px",
    border: "2px solid #e0e0e0",
    borderRadius: "8px",
    backgroundColor: "#f9f9f9",
    cursor: "pointer",
    transition: "all 0.2s ease",
    fontSize: "14px",
  },
  vendorButtonActive: {
    backgroundColor: "#4CAF50",
    color: "white",
    borderColor: "#45a049",
  },
  vendorButtonContent: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    textAlign: "center",
    gap: "4px",
  },
  medicinesHeading: {
    marginTop: "20px",
    fontSize: "16px",
  },
  medicineList: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
    maxHeight: "600px",
    overflowY: "auto",
  },
  medicineCard: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "12px",
    border: "1px solid #e0e0e0",
    borderRadius: "6px",
    backgroundColor: "#fafafa",
  },
  medicineCardContent: {
    flex: 1,
    display: "flex",
    flexDirection: "column",
    gap: "4px",
  },
  medicineCode: {
    color: "#666",
    fontSize: "12px",
  },
  priceTag: {
    backgroundColor: "#e3f2fd",
    color: "#1976d2",
    padding: "4px 8px",
    borderRadius: "4px",
    fontSize: "12px",
    fontWeight: "bold",
    alignSelf: "fit-content",
  },
  addButton: {
    padding: "8px 12px",
    backgroundColor: "#4CAF50",
    color: "white",
    border: "none",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "12px",
    marginLeft: "10px",
  },
  cartPanel: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
    backgroundColor: "#f5f5f5",
    padding: "15px",
    borderRadius: "8px",
    height: "fit-content",
    position: "sticky",
    top: "20px",
  },
  emptyCartText: {
    textAlign: "center",
    color: "#999",
    padding: "20px 10px",
  },
  cartItemsList: {
    display: "flex",
    flexDirection: "column",
    gap: "10px",
  },
  cartItem: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    padding: "10px",
    backgroundColor: "white",
    borderRadius: "6px",
    border: "1px solid #ddd",
  },
  cartItemMeta: {
    fontSize: "11px",
    color: "#999",
  },
  cartItemPrice: {
    fontSize: "12px",
    color: "#666",
  },
  quantityControl: {
    display: "flex",
    gap: "4px",
    alignItems: "center",
  },
  qtyBtn: {
    width: "28px",
    height: "28px",
    padding: "0",
    border: "1px solid #ddd",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "14px",
    backgroundColor: "white",
  },
  qtyInput: {
    width: "50px",
    textAlign: "center",
    border: "1px solid #ddd",
    borderRadius: "4px",
    padding: "4px",
  },
  removeBtn: {
    marginLeft: "auto",
    width: "28px",
    height: "28px",
    padding: "0",
    border: "1px solid #ff6b6b",
    borderRadius: "4px",
    cursor: "pointer",
    backgroundColor: "#ff6b6b",
    color: "white",
    fontSize: "14px",
  },
  cartItemTotal: {
    fontSize: "12px",
    fontWeight: "bold",
    color: "#333",
  },
  cartSummary: {
    padding: "12px",
    backgroundColor: "white",
    borderRadius: "6px",
    border: "1px solid #ddd",
  },
  summaryRow: {
    display: "flex",
    justifyContent: "space-between",
    fontSize: "13px",
    marginBottom: "6px",
  },
  divider: {
    height: "1px",
    backgroundColor: "#ddd",
    margin: "8px 0",
  },
  totalAmount: {
    fontSize: "16px",
    color: "#4CAF50",
  },
  checkoutSection: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  checkoutBtn: {
    padding: "10px",
    backgroundColor: "#4CAF50",
    color: "white",
    border: "none",
    borderRadius: "6px",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: "bold",
  },
  secondaryBtn: {
    padding: "8px",
    backgroundColor: "#f0f0f0",
    color: "#333",
    border: "1px solid #ddd",
    borderRadius: "4px",
    cursor: "pointer",
    fontSize: "12px",
  },
  actionsRow: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  historySection: {
    padding: "20px",
    borderTop: "1px solid #eee",
  },
  salesContainer: {
    display: "grid",
    gridTemplateColumns: "1fr 350px",
    gap: "20px",
    padding: "20px",
  },
  salesBrowsePanel: {
    display: "flex",
    flexDirection: "column",
    gap: "15px",
  },
  medicineSelectionCard: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "12px",
    border: "1px solid #e0e0e0",
    borderRadius: "6px",
    backgroundColor: "#fafafa",
  },
  saleSelectionRow: {
    display: "flex",
    gap: "8px",
    alignItems: "center",
  },
  saleQtyInput: {
    width: "60px",
    padding: "6px",
    border: "1px solid #ddd",
    borderRadius: "4px",
    textAlign: "center",
  },
  saleCartPanel: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
    backgroundColor: "#f5f5f5",
    padding: "15px",
    borderRadius: "8px",
    height: "fit-content",
    position: "sticky",
    top: "20px",
  },
  emptyText: {
    textAlign: "center",
    color: "#999",
    padding: "20px",
  },
};

export default CashierWorkspace;