import { useState, useEffect } from "react";
import { Printer, ClipboardList, Package } from "lucide-react";
import { getLowStock } from "../services/inventoryService";

export default function OrderGeneration() {
  const [orderList, setOrderList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchLowStock = async () => {
      try {
        const data = await getLowStock();
        setOrderList(data);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchLowStock();
  }, []);

  const executePrint = () => window.print();

  const totalOrderValue = orderList.reduce((s, i) => s + (i.toOrder * 10), 0); // estimated

  return (
    <div>
      <div className="view-header no-print">
        <div>
          <span style={{ color: "var(--accent)", fontSize: "0.72rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "1.5px" }}>End of Day Reporting</span>
          <h2>Auto-Generated Orders</h2>
          <p>The system has identified <span style={{ color: "var(--accent)" }}>{orderList.length} low-stock items</span>. Based on current inventory velocity, the following purchase orders have been drafted for your review.</p>
        </div>
      </div>

      {/* Summary Card */}
      <div className="no-print" style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: "16px", marginBottom: "28px" }}>
        <div className="glass-card" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <span style={{ color: "var(--text-secondary)", fontSize: "0.7rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "1px" }}>Replenishment Summary</span>
            <div style={{ fontSize: "1.6rem", fontWeight: 800, marginTop: "8px" }}>Total Order Value:<br/>${totalOrderValue.toLocaleString()}.00</div>
          </div>
          <button className="btn-primary" onClick={executePrint}>
            <Printer size={16} /> Print Order Summary
          </button>
        </div>
        <div className="glass-card" style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <Package size={18} color="var(--accent)" />
            <div>
              <div style={{ fontWeight: 700, fontSize: "0.95rem" }}>Next Sync</div>
              <span style={{ color: "var(--text-secondary)", fontSize: "0.8rem" }}>Scheduled for {new Date(Date.now() + 86400000).toLocaleDateString()}</span>
            </div>
          </div>
          <span className="badge info" style={{ width: "fit-content", marginTop: "auto" }}>Auto-Pilot</span>
        </div>
      </div>

      <h3 style={{ fontSize: "1.1rem", fontWeight: 700, margin: "0 0 16px" }} className="no-print">Draft Purchase Orders</h3>

      <div className="table-container no-print">
        <table className="data-table">
          <thead>
            <tr>
              <th>Item Code</th>
              <th>Trade Name</th>
              <th>Current Stock</th>
              <th>Qty to Order</th>
              <th>Target Vendor</th>
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan="5" style={{textAlign:"center", padding: "40px"}}>Loading Database...</td></tr> : null}
            {!loading && orderList.map((item, idx) => (
              <tr key={idx}>
                <td style={{fontFamily: "monospace", color: "var(--accent)"}}>{item.code}</td>
                <td style={{fontWeight: 600}}>{item.name}</td>
                <td style={{color: "var(--danger)", fontWeight: 700}}>{item.current}</td>
                <td style={{fontWeight: 700}}>{item.toOrder} Units</td>
                <td>{item.vendor}</td>
              </tr>
            ))}
            {!loading && orderList.length === 0 && (
              <tr><td colSpan="5" style={{ textAlign: "center", padding: "40px" }}>No items require ordering currently.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Printable Area */}
      <div className="printable-area">
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 20 }}>
          <ClipboardList size={32} />
          <h2 style={{ margin: 0 }}>End of Day Purchase Orders</h2>
        </div>
        <hr />
        <p><strong>Generated on:</strong> {new Date().toLocaleString()}</p>
        <p>Please dispatch the following quantities to the respective vendors:</p>
        <table style={{ width: "100%", textAlign: "left", borderCollapse: "collapse", marginTop: 20 }}>
          <thead>
            <tr>
              <th style={{padding: "10px", borderBottom: "2px solid #000"}}>Item Code</th>
              <th style={{padding: "10px", borderBottom: "2px solid #000"}}>Trade Name</th>
              <th style={{padding: "10px", borderBottom: "2px solid #000"}}>Qty Req.</th>
              <th style={{padding: "10px", borderBottom: "2px solid #000"}}>Vendor Name</th>
            </tr>
          </thead>
          <tbody>
            {orderList.map((item, idx) => (
              <tr key={idx}>
                <td style={{padding: "10px", borderBottom: "1px solid #ccc"}}>{item.code}</td>
                <td style={{padding: "10px", borderBottom: "1px solid #ccc"}}>{item.name}</td>
                <td style={{padding: "10px", borderBottom: "1px solid #ccc", fontWeight: "bold"}}>{item.toOrder}</td>
                <td style={{padding: "10px", borderBottom: "1px solid #ccc"}}>{item.vendor}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <p style={{ marginTop: 40, fontSize: "0.9rem", color: "#555" }}>
          Manager Signature _____________________
        </p>
      </div>
    </div>
  );
}
