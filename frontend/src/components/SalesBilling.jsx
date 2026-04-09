import { useState, useEffect, useRef } from "react";
import { makeSale, getRecentSales } from "../services/salesService";
import { getAllMedicines } from "../services/medicineService";
import { Printer, ShoppingCart, Trash2, Plus, CheckCircle, Clock, Receipt, Search } from "lucide-react";

export default function SalesBilling() {
  const [items, setItems] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedMedicine, setSelectedMedicine] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [receiptData, setReceiptData] = useState(null);
  const [toast, setToast] = useState(null);
  const [recentSales, setRecentSales] = useState([]);
  const [loadingSales, setLoadingSales] = useState(true);
  const [medicines, setMedicines] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);
  const inputRef = useRef(null);

  const fetchRecentSales = async () => {
    try {
      const data = await getRecentSales();
      setRecentSales(data);
    } catch (e) {
      console.error("Failed to load recent sales:", e);
    } finally {
      setLoadingSales(false);
    }
  };

  useEffect(() => {
    fetchRecentSales();
    const fetchMedicines = async () => {
      try {
        const data = await getAllMedicines();
        setMedicines(data);
      } catch (e) {
        console.error("Failed to load medicines:", e);
      }
    };
    fetchMedicines();
  }, []);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Filter medicines based on search query
  const filteredMedicines = medicines.filter((med) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      med.medicineCode?.toLowerCase().includes(q) ||
      med.tradeName?.toLowerCase().includes(q) ||
      med.genericName?.toLowerCase().includes(q)
    );
  });

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    setSelectedMedicine(null);
    setShowDropdown(true);
  };

  const handleSelectMedicine = (med) => {
    setSelectedMedicine(med);
    setSearchQuery(`${med.tradeName} (${med.medicineCode})`);
    setShowDropdown(false);
  };

  const addItem = () => {
    if (!selectedMedicine) return;
    setItems([
      ...items,
      {
        medicineCode: selectedMedicine.medicineCode,
        medicineName: selectedMedicine.tradeName,
        genericName: selectedMedicine.genericName,
        unitPrice: selectedMedicine.unitSellingPrice,
        quantity: Number(quantity),
      },
    ]);
    setSearchQuery("");
    setSelectedMedicine(null);
    setQuantity(1);
    inputRef.current?.focus();
  };

  const removeItem = (idx) => {
    setItems(items.filter((_, i) => i !== idx));
  };

  const checkout = async () => {
    if (items.length === 0) return;
    setIsSubmitting(true);
    try {
      const resp = await makeSale(
        items.map((i) => ({ medicineCode: i.medicineCode, quantity: i.quantity }))
      );
      const receipt = {
        invoiceNo: `INV-${Date.now().toString().slice(-6)}`,
        date: new Date().toLocaleString(),
        total: resp.totalAmount,
        items: [...items],
      };
      setReceiptData(receipt);
      setItems([]);
      fetchRecentSales();
    } catch (e) {
      setToast({ type: "error", msg: e.message });
      setTimeout(() => setToast(null), 3000);
    } finally {
      setIsSubmitting(false);
    }
  };

  const executePrint = () => {
    window.print();
  };

  const cartTotal = items.reduce((sum, i) => sum + (i.unitPrice || 0) * i.quantity, 0);

  return (
    <div>
      <div style={{ display: "grid", gridTemplateColumns: "1.3fr 1fr", gap: "24px", alignItems: "start" }} className="no-print">

        {/* Left: Input Panel */}
        <div>
          <h2 style={{ margin: "0 0 6px", fontSize: "1.75rem", fontWeight: 800, letterSpacing: "-0.5px" }}>Point of Sale</h2>
          <p style={{ color: "var(--text-secondary)", margin: "0 0 24px", fontSize: "0.92rem" }}>Process pharmacy transactions with clinical precision.</p>

          <div className="glass-card">
            <h3 style={{ marginTop: 0, marginBottom: "20px", display: "flex", alignItems: "center", gap: 8, fontSize: "1rem" }}>
              <ShoppingCart size={18} /> Add Item to Cart
            </h3>
            <div style={{ display: "grid", gridTemplateColumns: "1fr auto auto", gap: "12px", alignItems: "end" }}>

              {/* Searchable Medicine Picker */}
              <div className="form-group" ref={dropdownRef} style={{ position: "relative" }}>
                <label>Medicine (Name or Code)</label>
                <div style={{ position: "relative" }}>
                  <Search size={14} style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", opacity: 0.4, pointerEvents: "none" }} />
                  <input
                    ref={inputRef}
                    value={searchQuery}
                    onChange={handleSearchChange}
                    onFocus={() => setShowDropdown(true)}
                    placeholder="Search by name, generic, or code..."
                    style={{ paddingLeft: "34px" }}
                    autoFocus
                  />
                </div>
                {showDropdown && (
                  <div
                    style={{
                      position: "absolute",
                      top: "100%",
                      left: 0,
                      right: 0,
                      marginTop: "4px",
                      background: "var(--card-bg, #1a1e36)",
                      border: "1px solid var(--border-subtle, rgba(255,255,255,0.1))",
                      borderRadius: "8px",
                      maxHeight: "240px",
                      overflowY: "auto",
                      zIndex: 100,
                      boxShadow: "0 12px 40px rgba(0,0,0,0.5)",
                    }}
                  >
                    {filteredMedicines.length === 0 ? (
                      <div style={{ padding: "16px", textAlign: "center", color: "var(--text-secondary)", fontSize: "0.85rem" }}>
                        No medicines found
                      </div>
                    ) : (
                      filteredMedicines.map((med) => (
                        <div
                          key={med.medicineId}
                          onClick={() => handleSelectMedicine(med)}
                          style={{
                            padding: "10px 14px",
                            cursor: "pointer",
                            borderBottom: "1px solid rgba(255,255,255,0.05)",
                            transition: "background 0.15s",
                            background: selectedMedicine?.medicineId === med.medicineId ? "rgba(0,212,200,0.1)" : "transparent",
                          }}
                          onMouseEnter={(e) => (e.currentTarget.style.background = "rgba(0,212,200,0.08)")}
                          onMouseLeave={(e) => (e.currentTarget.style.background = selectedMedicine?.medicineId === med.medicineId ? "rgba(0,212,200,0.1)" : "transparent")}
                        >
                          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                            <div>
                              <span style={{ fontWeight: 700, fontSize: "0.92rem" }}>{med.tradeName}</span>
                              <span style={{ color: "var(--text-secondary)", fontSize: "0.8rem", marginLeft: 8 }}>({med.genericName})</span>
                            </div>
                            <span style={{
                              fontFamily: "monospace",
                              fontSize: "0.78rem",
                              padding: "2px 8px",
                              borderRadius: "4px",
                              background: "rgba(0,212,200,0.12)",
                              color: "var(--accent, #00D4C8)",
                              fontWeight: 600,
                            }}>
                              {med.medicineCode}
                            </span>
                          </div>
                          <div style={{ fontSize: "0.78rem", color: "var(--text-secondary)", marginTop: "3px" }}>
                            Unit Price: <span style={{ color: "var(--success, #22c55e)", fontWeight: 600 }}>${med.unitSellingPrice?.toFixed(2)}</span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>

              <div className="form-group">
                <label>Quantity</label>
                <input type="number" min="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} style={{ width: "80px" }} />
              </div>
              <button className="btn-primary" onClick={addItem} disabled={!selectedMedicine} style={{ height: "42px" }}>
                <Plus size={16} /> Add to Session
              </button>
            </div>

            {/* Selected medicine preview */}
            {selectedMedicine && (
              <div style={{
                marginTop: "14px",
                padding: "10px 14px",
                background: "rgba(0,212,200,0.06)",
                borderRadius: "8px",
                border: "1px solid rgba(0,212,200,0.15)",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                fontSize: "0.88rem"
              }}>
                <div>
                  <span style={{ fontWeight: 700 }}>{selectedMedicine.tradeName}</span>
                  <span style={{ color: "var(--text-secondary)", marginLeft: 6 }}>• {selectedMedicine.genericName}</span>
                  <span style={{ color: "var(--text-secondary)", marginLeft: 6 }}>• {selectedMedicine.medicineCode}</span>
                </div>
                <span style={{ fontWeight: 700, color: "var(--success, #22c55e)" }}>
                  ${selectedMedicine.unitSellingPrice?.toFixed(2)} / unit
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Cart Panel */}
        <div className="glass-card">
          <h3 style={{ marginTop: 0, display: "flex", alignItems: "center", gap: 8 }}>
            <ShoppingCart size={18} /> Current Cart
            {items.length > 0 && <span className="badge info" style={{ marginLeft: 8 }}>{items.length} Items</span>}
          </h3>
          {items.length === 0 ? (
            <p style={{ color: "var(--text-secondary)", textAlign: "center", padding: "40px 0" }}>Cart is empty</p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "0" }}>
              {items.map((item, idx) => (
                <div key={idx} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px 0", borderBottom: "1px solid var(--border-subtle)" }}>
                  <div style={{ flex: 1 }}>
                    <span style={{ fontWeight: 700, fontSize: "0.92rem" }}>{item.medicineName || item.medicineCode}</span>
                    <br />
                    <span style={{ color: "var(--text-secondary)", fontSize: "0.78rem" }}>
                      {item.medicineCode} • {item.quantity} × ${item.unitPrice?.toFixed(2)}
                    </span>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <span style={{ fontWeight: 700, fontSize: "0.9rem" }}>
                      ${(item.unitPrice * item.quantity).toFixed(2)}
                    </span>
                    <button onClick={() => removeItem(idx)} style={{ background: "transparent", color: "var(--danger)", border: "none", cursor: "pointer", padding: "4px" }}>
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {items.length > 0 && (
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "16px", padding: "12px 0", borderTop: "1px solid var(--border-subtle)" }}>
              <span style={{ fontWeight: 700, fontSize: "0.95rem" }}>Estimated Total</span>
              <span style={{ fontWeight: 800, fontSize: "1.15rem", color: "var(--success, #22c55e)" }}>${cartTotal.toFixed(2)}</span>
            </div>
          )}

          <div style={{ marginTop: items.length > 0 ? "8px" : "16px", paddingTop: items.length > 0 ? "0" : "16px", borderTop: items.length > 0 ? "none" : "1px solid var(--border-subtle)" }}>
            <button
              className="btn-primary"
              style={{ width: "100%", padding: "14px", fontSize: "0.95rem", background: items.length > 0 ? "var(--success)" : "rgba(255,255,255,0.05)", border: "none", fontWeight: 700, letterSpacing: "0.5px" }}
              onClick={checkout}
              disabled={items.length === 0 || isSubmitting}
            >
              <CheckCircle size={18} /> {isSubmitting ? "Processing..." : "COMPLETE CHECKOUT"}
            </button>
          </div>
        </div>
      </div>

      {/* Recent Sales Section */}
      <div className="glass-card no-print" style={{ marginTop: "24px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
          <div>
            <h3 style={{ margin: 0, display: "flex", alignItems: "center", gap: 8, fontSize: "1.1rem", fontWeight: 700 }}>
              <Receipt size={18} /> Recent Sales
            </h3>
            <span style={{ color: "var(--text-secondary)", fontSize: "0.82rem" }}>Last 20 transactions across all POS terminals</span>
          </div>
          <button
            className="btn-secondary"
            onClick={() => { setLoadingSales(true); fetchRecentSales(); }}
            style={{ fontSize: "0.82rem", padding: "6px 14px" }}
          >
            Refresh
          </button>
        </div>

        {loadingSales ? (
          <div style={{ textAlign: "center", padding: "40px 0", color: "var(--text-secondary)" }}>
            <Clock size={24} style={{ opacity: 0.5, marginBottom: 8 }} />
            <p>Loading recent sales...</p>
          </div>
        ) : recentSales.length === 0 ? (
          <div style={{ textAlign: "center", padding: "40px 0", color: "var(--text-secondary)" }}>
            <ShoppingCart size={24} style={{ opacity: 0.5, marginBottom: 8 }} />
            <p>No sales recorded yet.</p>
          </div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Sale ID</th>
                  <th>Date</th>
                  <th>Items</th>
                  <th style={{ textAlign: "right" }}>Total Amount</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {recentSales.map((sale) => (
                  <tr key={sale.saleId}>
                    <td style={{ fontWeight: 700, fontFamily: "monospace", fontSize: "0.9rem" }}>
                      #{sale.saleId}
                    </td>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                        <Clock size={13} style={{ opacity: 0.5 }} />
                        {new Date(sale.saleDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: "3px" }}>
                        {sale.items && sale.items.map((item, idx) => (
                          <span key={idx} style={{ fontSize: "0.82rem" }}>
                            <span style={{ fontWeight: 600 }}>{item.medicineName || item.medicineCode}</span>
                            <span style={{ color: "var(--text-secondary)", marginLeft: 4 }}>× {item.quantity}</span>
                            <span style={{ color: "var(--text-secondary)", marginLeft: 4 }}>@ ${item.unitPrice?.toFixed(2)}</span>
                          </span>
                        ))}
                        {(!sale.items || sale.items.length === 0) && (
                          <span style={{ color: "var(--text-secondary)", fontSize: "0.82rem" }}>—</span>
                        )}
                      </div>
                    </td>
                    <td style={{ textAlign: "right", fontWeight: 700, fontSize: "0.95rem", color: "var(--success)" }}>
                      ${sale.totalAmount?.toFixed(2)}
                    </td>
                    <td>
                      <span className="badge success">Completed</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modern Invoice Modal */}
      {receiptData && (
        <div className="modal-overlay no-print">
          <div className="modal-content" style={{ maxWidth: "450px", textAlign: "center" }}>
            <div style={{ width: 60, height: 60, borderRadius: "50%", background: "var(--success)", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 20px" }}>
              <ShoppingCart size={30} color="#fff" />
            </div>
            <h2 style={{ margin: "0 0 8px" }}>Payment Successful</h2>
            <p style={{ color: "var(--text-secondary)", margin: "0 0 30px" }}>Total Amount: <strong style={{ color: "var(--success)", fontSize: "1.2rem" }}>${receiptData.total.toFixed(2)}</strong></p>

            <button className="btn-primary" style={{ width: "100%", marginBottom: 12 }} onClick={executePrint}>
              <Printer size={18} /> Print Invoice
            </button>
            <button className="btn-secondary" style={{ width: "100%" }} onClick={() => setReceiptData(null)}>
              Start Next Sale
            </button>
          </div>
        </div>
      )}

      {/* Printable Invoice View */}
      {receiptData && (
        <div className="printable-area">
          <div style={{ textAlign: "center", marginBottom: 30 }}>
            <h1 style={{ margin: 0 }}>PHARMACY MSA</h1>
            <p style={{ margin: "5px 0" }}>123 Health Ave, Retail Park</p>
            <p style={{ margin: 0 }}>Tel: 555-0199</p>
          </div>
          <hr style={{ borderTop: "dashed 1px #000" }} />
          <p><strong>Invoice No:</strong> {receiptData.invoiceNo}</p>
          <p><strong>Date:</strong> {receiptData.date}</p>
          <table style={{ width: "100%", marginTop: 20 }}>
            <thead>
              <tr>
                <th style={{ textAlign: "left", paddingBottom: 10 }}>Item</th>
                <th style={{ textAlign: "center", paddingBottom: 10 }}>Qty</th>
                <th style={{ textAlign: "right", paddingBottom: 10 }}>Price</th>
              </tr>
            </thead>
            <tbody>
              {receiptData.items.map((i, idx) => (
                <tr key={idx}>
                  <td style={{ padding: "5px 0" }}>{i.medicineName || i.medicineCode}</td>
                  <td style={{ textAlign: "center" }}>{i.quantity}</td>
                  <td style={{ textAlign: "right" }}>${i.unitPrice ? (i.unitPrice * i.quantity).toFixed(2) : "--"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <hr style={{ borderTop: "dashed 1px #000", marginTop: 20 }} />
          <h2 style={{ textAlign: "right" }}>Total: ${receiptData.total.toFixed(2)}</h2>
          <p style={{ textAlign: "center", marginTop: 40 }}>Thank you for your business!</p>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className="toast-container no-print">
          <div className={`toast ${toast.type}`}>
            {toast.msg}
          </div>
        </div>
      )}
    </div>
  );
}
