import { useState, useEffect } from "react";
import { Plus, X, Search } from "lucide-react";
import { getAllVendors, createVendor } from "../services/vendorService";

export default function VendorManagement() {
  const [vendors, setVendors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  const [search, setSearch] = useState("");
  const [toast, setToast] = useState(null);
  
  const [form, setForm] = useState({ name: "", address: "", contact: "" });

  const fetchVendors = async () => {
    try {
      const data = await getAllVendors();
      setVendors(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVendors();
  }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    try {
      await createVendor(form);
      setToast({ type: "success", msg: "Vendor added successfully!" });
      setTimeout(() => setToast(null), 3000);
      setIsAdding(false);
      setForm({ name: "", address: "", contact: "" });
      fetchVendors(); // reload
    } catch (err) {
      setToast({ type: "error", msg: err.message });
      setTimeout(() => setToast(null), 3000);
    }
  };

  const filtered = vendors.filter(v => v.vendorName.toLowerCase().includes(search.toLowerCase()));

  return (
    <div>
      <div className="view-header">
        <div>
          <h2>Vendor Details</h2>
          <p>Manage pharmaceutical suppliers and their contact information.</p>
        </div>
        <button className="btn-primary" onClick={() => setIsAdding(true)}>
          <Plus size={18} /> Add Vendor
        </button>
      </div>

      <div className="search-wrapper">
        <Search size={18} className="search-icon" />
        <input 
          type="text" 
          placeholder="Search vendors by name..." 
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Vendor ID</th>
              <th>Company Name</th>
              <th>Physical Address</th>
              <th>Contact Node</th>
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan="4" style={{textAlign:"center", padding: "40px"}}>Loading Database...</td></tr> : null}
            {!loading && filtered.map((v) => (
              <tr key={v.vendorId}>
                <td style={{fontFamily: "monospace", color: "var(--accent)"}}>VND-{v.vendorId}</td>
                <td style={{fontWeight: 600}}>{v.vendorName}</td>
                <td>{v.address}</td>
                <td>{v.contactNo}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isAdding && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Register New Vendor</h3>
              <button className="close-btn" onClick={() => setIsAdding(false)}><X size={24} /></button>
            </div>
            
            <form className="dash-form" onSubmit={handleAdd}>
              <div className="form-group full-width">
                <label>Company Name</label>
                <input required value={form.name} onChange={e => setForm({...form, name: e.target.value})} />
              </div>
              <div className="form-group full-width">
                <label>Address</label>
                <input required value={form.address} onChange={e => setForm({...form, address: e.target.value})} />
              </div>
              <div className="form-group full-width">
                <label>Contact Email or Phone Number</label>
                <input required value={form.contact} onChange={e => setForm({...form, contact: e.target.value})} />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsAdding(false)}>Cancel</button>
                <button type="submit" className="btn-primary">Save Vendor</button>
              </div>
            </form>
          </div>
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
