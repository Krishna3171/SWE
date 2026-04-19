import { useState, useEffect } from "react";
import { Clock, AlertTriangle, Trash2 } from "lucide-react";
import { getExpiredBatches, dropBatch } from "../services/batchService";

export default function ExpiredMedicines() {
  const [expiredItems, setExpiredItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dropping, setDropping] = useState(null);
  const [toast, setToast] = useState(null);

  const showToast = (type, msg) => {
    setToast({ type, msg });
    setTimeout(() => setToast(null), 3500);
  };

  const fetchExpiry = async () => {
    try {
      const data = await getExpiredBatches();
      setExpiredItems(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchExpiry(); }, []);

  const handleDrop = async (item) => {
    if (!window.confirm(`Drop batch "${item.batch}" of ${item.name}? This will remove it from inventory.`)) return;
    setDropping(item.batchId);
    try {
      await dropBatch(item.batchId);
      showToast("success", `Batch ${item.batch} dropped — inventory updated.`);
      setExpiredItems(prev => prev.filter(i => i.batchId !== item.batchId));
    } catch (e) {
      showToast("error", e.message);
    } finally {
      setDropping(null);
    }
  };

  return (
    <div>
      <div className="view-header">
        <div>
          <h2>Expired Stock Report</h2>
          <p>Daily report of medicines past their shelf life.</p>
        </div>
      </div>

      <div className="stats-grid" style={{ gridTemplateColumns: "repeat(3, 1fr)" }}>
        <div className="stat-card glass-card danger">
          <div className="stat-icon"><AlertTriangle size={24} /></div>
          <div>
            <h3>Total Expired Items</h3>
            <div className="value">{expiredItems.reduce((acc, i) => acc + i.qty, 0)} Units</div>
          </div>
        </div>
        <div className="stat-card glass-card warning">
          <div className="stat-icon"><Clock size={24} /></div>
          <div>
            <h3>Pending Vendor Returns</h3>
            {/* simple discrete count of unique vendors in this list */}
            <div className="value">{new Set(expiredItems.map(i => i.vendor)).size} Vendors</div>
          </div>
        </div>
      </div>

      <div className="table-container" style={{ marginTop: "32px", border: "1px solid rgba(239, 68, 68, 0.3)" }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Item Code</th>
              <th>Trade Name</th>
              <th>Batch Number</th>
              <th>Expired Date</th>
              <th>Loss Qty</th>
              <th>Origin Vendor</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan="7" style={{textAlign:"center", padding: "40px"}}>Loading Database...</td></tr> : null}
            {!loading && expiredItems.map((item, idx) => (
              <tr key={idx}>
                <td style={{fontFamily: "monospace", color: "var(--accent)"}}>{item.code}</td>
                <td style={{fontWeight: 600}}>{item.name}</td>
                <td>{item.batch}</td>
                <td style={{color: "var(--danger)"}}>{item.date}</td>
                <td style={{fontWeight: 700}}>{item.qty} Units</td>
                <td>{item.vendor}</td>
                <td>
                  <button
                    className="btn-secondary"
                    style={{ padding: "4px 12px", fontSize: "0.8rem", color: "var(--danger)", borderColor: "var(--danger)", display: "flex", alignItems: "center", gap: "4px" }}
                    onClick={() => handleDrop(item)}
                    disabled={dropping === item.batchId}
                  >
                    <Trash2 size={13} />
                    {dropping === item.batchId ? "Dropping..." : "Drop"}
                  </button>
                </td>
              </tr>
            ))}
            {!loading && expiredItems.length === 0 && (
              <tr><td colSpan="7" style={{ textAlign: "center", padding: "40px" }}>No expired items found!</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {toast && (
        <div className="toast-container">
          <div className={`toast ${toast.type}`}>{toast.msg}</div>
        </div>
      )}
    </div>
  );
}
