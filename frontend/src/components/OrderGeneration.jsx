import { useState, useEffect } from "react";
import { Printer, ClipboardList, X } from "lucide-react";
import { getLowStock } from "../services/inventoryService";
import { getPendingPurchases, processPurchase, receivePurchaseLine } from "../services/purchaseService";

const LOW_STOCK_DATES_KEY = "lowStockDates";

function getStoredDates() {
  try { return JSON.parse(localStorage.getItem(LOW_STOCK_DATES_KEY) || "{}"); }
  catch { return {}; }
}

function recordDates(codes) {
  const stored = getStoredDates();
  const today = new Date().toISOString().split("T")[0];
  codes.forEach(code => { if (!stored[code]) stored[code] = today; });
  localStorage.setItem(LOW_STOCK_DATES_KEY, JSON.stringify(stored));
  return stored;
}

export default function OrderGeneration() {

  const [lowStock, setLowStock] = useState([]);
  const [lowStockError, setLowStockError] = useState(null);
  const [pendingOrders, setPendingOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [receiveModal, setReceiveModal] = useState(null); // low-stock item being received
  const [receiveForm, setReceiveForm] = useState({ batchNumber: "", expiryDate: "", quantity: "", unitPurchasePrice: "", vendorId: "" });
  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (type, msg) => {
    setToast({ type, msg });
    setTimeout(() => setToast(null), 3500);
  };

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [pendingData, lowData] = await Promise.allSettled([
        getPendingPurchases(),
        getLowStock()
      ]);
      setPendingOrders(pendingData.status === "fulfilled" ? pendingData.value : []);
      if (lowData.status === "fulfilled") {
        setLowStockError(null);
        const rawLow = lowData.value;
        const dates = recordDates(rawLow.map(i => i.code));
        setLowStock(rawLow.map(i => ({ ...i, dateAdded: dates[i.code] })));
      } else {
        setLowStockError(lowData.reason?.message || "Failed to load low stock data");
        setLowStock([]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []); // eslint-disable-line

  // IDs of medicines already covered by a pending order (medicineCode basis not available,
  // but low-stock uses medicine code while pending uses medicineId — keep both sections separate)

  const openReceiveModal = (item) => {
    const vendorNum = parseInt(item.vendor.replace(/\D/g, ""), 10) || "";
    setReceiveForm({
      batchNumber: "",
      expiryDate: "",
      quantity: String(item.toOrder),
      unitPurchasePrice: "",
      vendorId: String(vendorNum)
    });
    setReceiveModal(item);
  };

  const handleReceiveSupply = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const vendorId = parseInt(receiveForm.vendorId, 10);
      const purchaseResp = await processPurchase(vendorId, [{
        medicineCode: receiveModal.code,
        batchNumber: receiveForm.batchNumber,
        expiryDate: receiveForm.expiryDate,
        quantity: parseInt(receiveForm.quantity, 10),
        unitPurchasePrice: parseFloat(receiveForm.unitPurchasePrice)
      }]);

      // Fetch pending to find the batchId for the newly created purchase
      const allPending = await getPendingPurchases();
      const newEntry = allPending.find(p => p.purchaseId === purchaseResp.purchaseId);
      if (!newEntry) throw new Error("Could not find the created purchase order to receive.");

      await receivePurchaseLine(newEntry.purchaseId, newEntry.batchId);

      setReceiveModal(null);
      showToast("success", `Supply received — inventory updated for ${receiveModal.name}.`);
      fetchAll();
    } catch (err) {
      showToast("error", err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const executePrint = () => window.print();

  const totalOrderValue = pendingOrders
    .filter(i => !i.received)
    .reduce((s, i) => s + (i.quantity * i.unitPrice), 0);

  return (
    <div>
      <div className="view-header no-print">
        <div>
          <span style={{ color: "var(--accent)", fontSize: "0.72rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "1.5px" }}>Purchase Orders</span>
          <h2>Purchase Order List</h2>
          <p>Low-stock items below show medicines needing replenishment. Tick Received on a pending order when a shipment arrives — inventory is updated automatically.</p>
        </div>
      </div>

      {/* Summary Card */}
      <div className="no-print" style={{ marginBottom: "28px" }}>
        <div className="glass-card" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <span style={{ color: "var(--text-secondary)", fontSize: "0.7rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "1px" }}>Pending Orders Value</span>
            <div style={{ fontSize: "1.6rem", fontWeight: 800, marginTop: "8px" }}>
              Total Order Value:<br />${totalOrderValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </div>
          </div>
          <button className="btn-primary" onClick={executePrint}>
            <Printer size={16} /> Print Order Summary
          </button>
        </div>
      </div>

      {/* ── Low-Stock Items ── */}
      <h3 style={{ fontSize: "1.1rem", fontWeight: 700, margin: "0 0 12px" }} className="no-print">
        Low-Stock Items
        {lowStock.length > 0 && <span className="badge danger" style={{ marginLeft: 10, verticalAlign: "middle" }}>{lowStock.length} items</span>}
      </h3>
      <div className="table-container no-print" style={{ marginBottom: "32px" }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Medicine Code</th>
              <th>Trade Name</th>
              <th>Current Stock</th>
              <th>To Order</th>
              <th>Vendor</th>
              <th>Date Added</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} style={{ textAlign: "center", padding: "40px" }}>Loading...</td></tr>
            ) : lowStockError ? (
              <tr><td colSpan={7} style={{ textAlign: "center", padding: "40px", color: "var(--danger)" }}>{lowStockError}</td></tr>
            ) : lowStock.length === 0 ? (
              <tr><td colSpan={7} style={{ textAlign: "center", padding: "40px" }}>All stock levels are adequate.</td></tr>
            ) : lowStock.map((item, idx) => (
              <tr key={idx}>
                <td style={{ fontFamily: "monospace", color: "var(--accent)" }}>{item.code}</td>
                <td style={{ fontWeight: 600 }}>{item.name}</td>
                <td style={{ color: "var(--danger)", fontWeight: 700 }}>{item.current}</td>
                <td style={{ fontWeight: 700 }}>{item.toOrder} units</td>
                <td>{item.vendor}</td>
                <td style={{ color: "var(--text-secondary)", fontSize: "0.85rem" }}>{item.dateAdded}</td>
                <td>
                  <button className="btn-primary" style={{ padding: "4px 12px", fontSize: "0.8rem" }} onClick={() => openReceiveModal(item)}>
                    Receive Supply
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>


      {/* ── Receive Supply Modal (admin, for low-stock items) ── */}
      {receiveModal && (
        <div className="modal-overlay no-print">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Receive Supply — {receiveModal.name}</h3>
              <button className="close-btn" onClick={() => setReceiveModal(null)}><X size={24} /></button>
            </div>
            <form className="dash-form" onSubmit={handleReceiveSupply}>
              <div className="form-group">
                <label>Medicine Code</label>
                <input value={receiveModal.code} disabled style={{ opacity: 0.6 }} />
              </div>
              <div className="form-group">
                <label>Vendor ID</label>
                <input type="number" required value={receiveForm.vendorId} onChange={e => setReceiveForm({ ...receiveForm, vendorId: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Batch Number</label>
                <input required placeholder="e.g. BCH-001" value={receiveForm.batchNumber} onChange={e => setReceiveForm({ ...receiveForm, batchNumber: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Expiry Date</label>
                <input type="date" required value={receiveForm.expiryDate} onChange={e => setReceiveForm({ ...receiveForm, expiryDate: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Quantity</label>
                <input type="number" required value={receiveForm.quantity} onChange={e => setReceiveForm({ ...receiveForm, quantity: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Unit Purchase Price ($)</label>
                <input type="number" step="0.01" required value={receiveForm.unitPurchasePrice} onChange={e => setReceiveForm({ ...receiveForm, unitPurchasePrice: e.target.value })} />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setReceiveModal(null)}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={submitting}>
                  {submitting ? "Processing..." : "Confirm & Receive"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Printable Area — only today's low-stock items */}
      {(() => {
        const today = new Date().toISOString().split("T")[0];
        const todayItems = lowStock.filter(i => i.dateAdded === today);
        return (
          <div className="printable-area">
            <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 20 }}>
              <ClipboardList size={32} />
              <h2 style={{ margin: 0 }}>Purchase Order Summary</h2>
            </div>
            <hr />
            <p><strong>Generated on:</strong> {new Date().toLocaleString()}</p>
            <p><strong>Date:</strong> {today} &nbsp;|&nbsp; <strong>Items requiring replenishment today:</strong> {todayItems.length}</p>
            {todayItems.length === 0 ? (
              <p style={{ marginTop: 20, color: "#555" }}>No low-stock items were flagged today.</p>
            ) : (
              <table style={{ width: "100%", textAlign: "left", borderCollapse: "collapse", marginTop: 20 }}>
                <thead>
                  <tr>
                    <th style={{ padding: "10px", borderBottom: "2px solid #000" }}>Medicine Code</th>
                    <th style={{ padding: "10px", borderBottom: "2px solid #000" }}>Trade Name</th>
                    <th style={{ padding: "10px", borderBottom: "2px solid #000" }}>Current Stock</th>
                    <th style={{ padding: "10px", borderBottom: "2px solid #000" }}>To Order</th>
                    <th style={{ padding: "10px", borderBottom: "2px solid #000" }}>Vendor</th>
                    <th style={{ padding: "10px", borderBottom: "2px solid #000" }}>Date Added</th>
                  </tr>
                </thead>
                <tbody>
                  {todayItems.map((item, idx) => (
                    <tr key={idx}>
                      <td style={{ padding: "10px", borderBottom: "1px solid #ccc", fontFamily: "monospace" }}>{item.code}</td>
                      <td style={{ padding: "10px", borderBottom: "1px solid #ccc" }}>{item.name}</td>
                      <td style={{ padding: "10px", borderBottom: "1px solid #ccc" }}>{item.current}</td>
                      <td style={{ padding: "10px", borderBottom: "1px solid #ccc" }}>{item.toOrder} units</td>
                      <td style={{ padding: "10px", borderBottom: "1px solid #ccc" }}>{item.vendor}</td>
                      <td style={{ padding: "10px", borderBottom: "1px solid #ccc" }}>{item.dateAdded}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <p style={{ marginTop: 40, fontSize: "0.9rem", color: "#555" }}>
              Manager Signature _____________________
            </p>
          </div>
        );
      })()}

      {toast && (
        <div className="toast-container no-print">
          <div className={`toast ${toast.type}`}>{toast.msg}</div>
        </div>
      )}
    </div>
  );
}
