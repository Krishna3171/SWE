import { useState, useEffect } from "react";
import { getAllMedicines, addMedicine } from "../services/medicineService";
import { Search, Plus, X, Pill } from "lucide-react";

export default function MedicineManagement() {
  const [medicines, setMedicines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  const [search, setSearch] = useState("");
  
  const [form, setForm] = useState({ tradeName: "", genericName: "", unitSellingPrice: "", unitPurchasePrice: "" });
  const [toast, setToast] = useState(null);

  const fetchMedicines = async ({ throwOnError = false } = {}) => {
    try {
      const data = await getAllMedicines();
      setMedicines(data);
      return data;
    } catch (error) {
      console.error(error);
      if (throwOnError) {
        throw error;
      }
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMedicines();
  }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    console.log("Submitting medicine form:", form);
    try {
      const created = await addMedicine(form);
      console.log("Add medicine response:", created);
      if (created?.medicineCode) {
        setMedicines((prev) => {
          const exists = prev.some((item) => item.medicineCode === created.medicineCode);
          if (exists) return prev;
          return [
            {
              medicineId: Date.now(),
              medicineCode: created.medicineCode,
              tradeName: form.tradeName,
              genericName: form.genericName,
              unitSellingPrice: Number.parseFloat(form.unitSellingPrice || "0"),
              unitPurchasePrice: Number.parseFloat(form.unitPurchasePrice || "0"),
            },
            ...prev,
          ];
        });
        setIsAdding(false);
        setSearch("");
        setForm({ tradeName: "", genericName: "", unitSellingPrice: "", unitPurchasePrice: "" });
        setToast({ type: "success", msg: "Medicine Added Successfully!" });
        setTimeout(() => setToast(null), 3000);
        fetchMedicines();
      } else {
        throw new Error("Failed to add medicine: No verification code received");
      }
    } catch (e) {
      console.error("Add medicine error:", e);
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
          <button className="btn-primary" onClick={() => setIsAdding(true)} data-testid="add-medicine-header-btn">
            <Plus size={16} /> Add Medicine
          </button>
        ) : null}
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
                <input id="tradeName" name="tradeName" data-testid="trade-name-input" value={form.tradeName} onChange={e => setForm({...form, tradeName: e.target.value})} required placeholder="e.g. Tylenol" />
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

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsAdding(false)}>Cancel</button>
                <button type="submit" className="btn-primary" data-testid="submit-medicine-btn">Add Medicine</button>
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
