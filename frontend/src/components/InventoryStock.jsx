import { useState, useEffect } from "react";
import { Plus, X, Search, Printer, PackageOpen } from "lucide-react";
import { getInventory } from "../services/inventoryService";
import { processPurchase } from "../services/purchaseService";

export default function InventoryStock() {
  const [stockInfo, setStockInfo] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [search, setSearch] = useState("");
  const [isReceiving, setIsReceiving] = useState(false);
  const [printReceipt, setPrintReceipt] = useState(null);
  const [toast, setToast] = useState(null);
  
  const [supplyForm, setSupplyForm] = useState({
    code: "", quantity: "", batch: "", expiry: "", vendor: "", price: ""
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const data = await getInventory();
      setStockInfo(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleReceive = async (e) => {
    e.preventDefault();
    try {
      // the vendor id would normally be selected from a dropdown, parse int here
      const vId = parseInt(supplyForm.vendor.replace(/\D/g, ''), 10) || 1;
      
      const item = {
        medicineCode: supplyForm.code,
        batchNumber: supplyForm.batch,
        expiryDate: supplyForm.expiry,
        quantity: parseInt(supplyForm.quantity),
        unitPurchasePrice: parseFloat(supplyForm.price)
      };

      const resp = await processPurchase(vId, [item]);

      const receiptData = { ...supplyForm, date: new Date().toLocaleString(), receiptId: resp.purchaseId };
      setPrintReceipt(receiptData);
      setIsReceiving(false);
      setSupplyForm({ code: "", quantity: "", batch: "", expiry: "", vendor: "", price: "" });
      fetchData(); // reload table
    } catch (e) {
      setToast({ type: "error", msg: e.message });
      setTimeout(() => setToast(null), 3000);
    }
  };

  const executePrint = () => {
    window.print();
  };

  const filteredStock = stockInfo.filter(item => 
    item.code.toLowerCase().includes(search.toLowerCase()) || 
    item.tradeName.toLowerCase().includes(search.toLowerCase())
  );

  const lowStockCount = stockInfo.filter(i => i.currentStock < i.threshold).length;
  const totalStock = stockInfo.reduce((s, i) => s + i.currentStock, 0);

  return (
    <div>
      <div className="view-header no-print">
        <div>
          <h2>Inventory & Stock</h2>
          <p>Manage and monitor your pharmaceutical supply levels with real-time analytics and stock tracking.</p>
        </div>
        <button className="btn-primary" onClick={() => setIsReceiving(true)}>
          <Plus size={16} /> Receive Supply
        </button>
      </div>

      <div className="stats-grid no-print" style={{ gridTemplateColumns: "repeat(3, 1fr)" }}>
        <div className="stat-card glass-card accent">
          <div className="stat-label-row">
            <div className="stat-icon"><PackageOpen size={16} /></div>
            <h3>Total Inventory</h3>
          </div>
          <div className="value">{totalStock.toLocaleString()}</div>
        </div>
        <div className="stat-card glass-card danger">
          <div className="stat-label-row">
            <h3>Low Stock Alerts</h3>
          </div>
          <div className="value">{lowStockCount}</div>
          {lowStockCount > 0 && <span className="badge danger" style={{width:"fit-content"}}>Requires Action</span>}
        </div>
        <div className="stat-card glass-card success">
          <div className="stat-label-row">
            <h3>Storage Capacity</h3>
          </div>
          <div className="value">{stockInfo.length > 0 ? Math.round(((stockInfo.length - lowStockCount) / stockInfo.length) * 100) : 100}%</div>
        </div>
      </div>

      <h3 style={{ fontSize: "1.1rem", fontWeight: 700, margin: "0 0 16px" }}>Current Stock Levels</h3>

      <div className="search-wrapper no-print">
        <Search size={16} className="search-icon" />
        <input 
          type="text" 
          placeholder="Search inventory code or name..." 
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div className="table-container no-print">
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Trade Name</th>
              <th>Rack Number</th>
              <th>Current Stock</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan="5" style={{textAlign:"center", padding: "40px"}}>Loading Database...</td></tr> : null}
            {!loading && filteredStock.map((item, idx) => {
              const isLow = item.currentStock < item.threshold;
              return (
                <tr key={idx} style={{ background: isLow ? "rgba(239, 68, 68, 0.05)" : "" }}>
                  <td style={{fontFamily: "monospace", color: "var(--accent)"}}>{item.code}</td>
                  <td style={{fontWeight: 600}}>{item.tradeName}</td>
                  <td>{item.rack}</td>
                  <td style={{ fontWeight: 700, color: isLow ? "var(--danger)" : "var(--text-primary)" }}>
                    {item.currentStock}
                  </td>
                  <td>
                    {isLow ? <span className="badge danger">Low Stock</span> : <span className="badge success">Adequate</span>}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {isReceiving && (
        <div className="modal-overlay no-print">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Load New Supply</h3>
              <button className="close-btn" onClick={() => setIsReceiving(false)}><X size={24} /></button>
            </div>
            
            <form className="dash-form" onSubmit={handleReceive}>
              <div className="form-group">
                <label>Medicine Code</label>
                <input required placeholder="e.g. MED1" value={supplyForm.code} onChange={e => setSupplyForm({...supplyForm, code: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Quantity Received</label>
                <input type="number" required value={supplyForm.quantity} onChange={e => setSupplyForm({...supplyForm, quantity: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Batch Number</label>
                <input required placeholder="BCH-..." value={supplyForm.batch} onChange={e => setSupplyForm({...supplyForm, batch: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Expiry Date</label>
                <input type="date" required value={supplyForm.expiry} onChange={e => setSupplyForm({...supplyForm, expiry: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Total Unit Price ($)</label>
                <input type="number" step="0.01" required value={supplyForm.price} onChange={e => setSupplyForm({...supplyForm, price: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Vendor ID Num</label>
                <input required placeholder="e.g. 1" value={supplyForm.vendor} onChange={e => setSupplyForm({...supplyForm, vendor: e.target.value})} />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsReceiving(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Generate Receipt & Save</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {printReceipt && (
        <div className="modal-overlay no-print">
          <div className="modal-content" style={{ maxWidth: "400px", textAlign: "center" }}>
            <h3 style={{ color: "var(--success)", marginBottom: 10 }}>Supply Logged Globally!</h3>
            <p style={{ color: "var(--text-secondary)", marginBottom: 24 }}>System has recorded the Vendor Purchase ID: {printReceipt.receiptId}</p>
            
            <button className="btn-primary" style={{ width: "100%", marginBottom: 12 }} onClick={executePrint}>
              <Printer size={18} /> Print Supply Receipt
            </button>
            <button className="btn-secondary" style={{ width: "100%" }} onClick={() => setPrintReceipt(null)}>
              Close
            </button>
          </div>
        </div>
      )}

      {/* Actual Printable Area */}
      {printReceipt && (
        <div className="printable-area">
          <h2>MSA System - Supply Receipt</h2>
          <hr />
          <p><strong>Purchase Request ID:</strong> {printReceipt.receiptId}</p>
          <p><strong>Date/Time:</strong> {printReceipt.date}</p>
          <br/>
          <table style={{ width: "100%", textAlign: "left", borderCollapse: "collapse" }}>
            <tbody>
              <tr><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>Medicine Code</td><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>{printReceipt.code}</td></tr>
              <tr><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>Quantity Added</td><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>{printReceipt.quantity}</td></tr>
              <tr><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>Batch No</td><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>{printReceipt.batch}</td></tr>
              <tr><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>Expiry Date</td><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>{printReceipt.expiry}</td></tr>
              <tr><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>Vendor ID</td><td style={{padding: "8px 0", borderBottom: "1px solid #ccc"}}>{printReceipt.vendor}</td></tr>
            </tbody>
          </table>
          <p style={{ marginTop: 40, textAlign: "center", fontSize: "0.9rem", color: "#555" }}>
            Authorized Signature _____________________
          </p>
        </div>
      )}

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
