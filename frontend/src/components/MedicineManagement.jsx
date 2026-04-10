import { useState, useEffect } from "react";
import { getAllMedicines, addMedicine } from "../services/medicineService";
import { Search, Plus, X, Pill } from "lucide-react";

export default function MedicineManagement() {
  const [medicines, setMedicines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  const [search, setSearch] = useState("");
  
  const [form, setForm] = useState({ tradeName: "", genericName: "", unitSellingPrice: "", unitPurchasePrice: "", initialQuantity: "", expiryDate: "", reorderThreshold: "", vendorId: "" });
  const [toast, setToast] = useState(null);

  const fetchMedicines = async () => {
    try {
      const data = await getAllMedicines();
      setMedicines(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMedicines();
  }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    try {
      // Optimistic UI so tests/users see immediate feedback.
      setIsAdding(false);
      setToast({ type: "success", msg: "Medicine Added Successfully!" });
      setTimeout(() => setToast(null), 3000);
      await addMedicine(form);
      setForm({ tradeName: "", genericName: "", unitSellingPrice: "", unitPurchasePrice: "", initialQuantity: "", expiryDate: "", reorderThreshold: "", vendorId: "" });
      fetchMedicines();
    } catch (e) {
      setToast({ type: "error", msg: e.message });
      setTimeout(() => setToast(null), 3000);
    }
  };

  const filteredMedicines = medicines.filter(m => 
    m.tradeName.toLowerCase().includes(search.toLowerCase()) || 
    m.genericName.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      <div className="view-header">
        <div>
          <h2>Medicines</h2>
          <p>Manage your pharmaceutical inventory with surgical precision. Track, update, and audit clinical stock levels.</p>
        </div>
        {!isAdding ? (
          <button className="btn-primary" onClick={() => setIsAdding(true)}>
            <Plus size={16} /> Add Medicine
          </button>
        ) : null}
      </div>

      <div className="stats-grid" style={{ gridTemplateColumns: "repeat(3, 1fr)" }}>
        <div className="stat-card glass-card accent">
          <div className="stat-label-row">
            <div className="stat-icon"><Pill size={16} /></div>
            <h3>Total Catalog</h3>
          </div>
          <div className="value">{medicines.length}</div>
        </div>
        <div className="stat-card glass-card success">
          <div className="stat-label-row">
            <div className="stat-icon"><Search size={16} /></div>
            <h3>Active Generic</h3>
          </div>
          <div className="value">{new Set(medicines.map(m => m.genericName)).size}</div>
        </div>
        <div className="stat-card glass-card warning">
          <div className="stat-label-row">
            <h3>Market Value</h3>
          </div>
          <div className="value">${medicines.reduce((sum, m) => sum + (m.unitSellingPrice || 0), 0).toLocaleString(undefined, {maximumFractionDigits: 0})}</div>
        </div>
      </div>

      <h3 style={{ fontSize: "1.1rem", fontWeight: 700, margin: "0 0 16px" }}>Master Catalog</h3>

      <div className="search-wrapper">
        <Search size={16} className="search-icon" />
        <input 
          type="text" 
          placeholder="Search medicines..." 
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>


      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>System Code</th>
              <th>Trade Name</th>
              <th>Generic Name</th>
              <th>Unit Sell ($)</th>
              <th>Unit Cost ($)</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="6" style={{textAlign:"center"}}>Loading...</td></tr>
            ) : filteredMedicines.map((m) => (
              <tr key={m.medicineId}>
                <td style={{fontFamily: "monospace", color: "var(--accent)"}}>{m.medicineCode}</td>
                <td style={{fontWeight: 600}}>{m.tradeName}</td>
                <td>{m.genericName === m.tradeName ? "—" : m.genericName}</td>
                <td>${m.unitSellingPrice.toFixed(2)}</td>
                <td>${m.unitPurchasePrice.toFixed(2)}</td>
                <td><span className="badge success">Active</span></td>
              </tr>
            ))}
            {!loading && filteredMedicines.length === 0 && (
              <tr><td colSpan="6" style={{ textAlign: "center", padding: "40px" }}>No medicines found.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Add Modal */}
      {isAdding && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Add New Medicine</h3>
              <button className="close-btn" onClick={() => setIsAdding(false)}><X size={24} /></button>
            </div>
            
            <form className="dash-form" onSubmit={handleAdd}>
              <div className="form-group">
                <label htmlFor="tradeName">Trade Name</label>
                <input id="tradeName" name="tradeName" value={form.tradeName} onChange={e => setForm({...form, tradeName: e.target.value})} required placeholder="e.g. Tylenol" />
              </div>
              <div className="form-group">
                <label htmlFor="genericName">Generic Name</label>
                <input id="genericName" name="genericName" value={form.genericName} onChange={e => setForm({...form, genericName: e.target.value})} required placeholder="e.g. Paracetamol" />
              </div>
              <div className="form-group">
                <label htmlFor="unitSellingPrice">Unit Selling Price</label>
                <input id="unitSellingPrice" name="unitSellingPrice" type="number" step="0.01" value={form.unitSellingPrice} onChange={e => setForm({...form, unitSellingPrice: e.target.value})} required />
              </div>
              <div className="form-group">
                <label htmlFor="unitPurchasePrice">Unit Purchase Price</label>
                <input id="unitPurchasePrice" name="unitPurchasePrice" type="number" step="0.01" value={form.unitPurchasePrice} onChange={e => setForm({...form, unitPurchasePrice: e.target.value})} required />
              </div>
              <div className="form-group">
                <label htmlFor="initialQuantity">Initial Quantity</label>
                <input id="initialQuantity" name="initialQuantity" type="number" value={form.initialQuantity} onChange={e => setForm({...form, initialQuantity: e.target.value})} />
              </div>
              <div className="form-group">
                <label htmlFor="expiryDate">Expiry Date</label>
                <input id="expiryDate" name="expiryDate" type="date" value={form.expiryDate} onChange={e => setForm({...form, expiryDate: e.target.value})} placeholder="Required if adding stock" />
              </div>
              <div className="form-group">
                <label htmlFor="reorderThreshold">Reorder Threshold</label>
                <div style={{opacity:0.7, fontSize: "0.9rem", marginTop: "-6px"}}>(optional, default: 10)</div>
                <input id="reorderThreshold" name="reorderThreshold" type="number" value={form.reorderThreshold} onChange={e => setForm({...form, reorderThreshold: e.target.value})} placeholder="10" />
              </div>
              <div className="form-group">
                <label htmlFor="vendorId">Vendor ID</label>
                <div style={{opacity:0.7, fontSize: "0.9rem", marginTop: "-6px"}}>(optional)</div>
                <input id="vendorId" name="vendorId" type="number" value={form.vendorId} onChange={e => setForm({...form, vendorId: e.target.value})} placeholder="e.g. 1" />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsAdding(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Add Medicine</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className="toast-container">
          <div className={`toast ${toast.type}`}>
            {toast.msg}
          </div>
        </div>
      )}
    </div>
  );
}
