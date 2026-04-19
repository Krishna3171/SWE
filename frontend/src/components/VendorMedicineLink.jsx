import { useState, useEffect } from "react";
import { Link2, CheckCircle2 } from "lucide-react";
import { getAllMedicines } from "../services/medicineService";
import { getAllVendors } from "../services/vendorService";
import { getVendorMedicineLinks, linkVendorToMedicine } from "../services/vendorMedicineService";

export default function VendorMedicineLink() {
  const [medicines, setMedicines] = useState([]);
  const [vendors, setVendors] = useState([]);
  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedVendor, setSelectedVendor] = useState("");
  const [selectedMedicine, setSelectedMedicine] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (type, msg) => {
    setToast({ type, msg });
    setTimeout(() => setToast(null), 3500);
  };

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [medsData, vendorsData] = await Promise.all([
        getAllMedicines(),
        getAllVendors(),
      ]);
      setMedicines(medsData);
      setVendors(vendorsData);

      // existing links — only available after backend restart with new route
      try {
        const linksData = await getVendorMedicineLinks();
        setLinks(linksData);
      } catch {
        setLinks([]);
      }
    } catch (e) {
      showToast("error", e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []); // eslint-disable-line

  const isAlreadyLinked = (vendorId, medicineId) =>
    links.some(l => l.vendorId === vendorId && l.medicineId === medicineId);

  const handleLink = async (e) => {
    e.preventDefault();
    const vId = parseInt(selectedVendor, 10);
    const mId = parseInt(selectedMedicine, 10);

    if (!vId || !mId) {
      showToast("error", "Please select both a vendor and a medicine.");
      return;
    }

    if (isAlreadyLinked(vId, mId)) {
      showToast("error", "This vendor is already linked to that medicine.");
      return;
    }

    setSubmitting(true);
    try {
      await linkVendorToMedicine(vId, mId);
      showToast("success", "Vendor linked to medicine successfully.");
      setSelectedVendor("");
      setSelectedMedicine("");
      fetchAll();
    } catch (err) {
      showToast("error", err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const getVendorName = (id) => vendors.find(v => v.vendorId === id)?.vendorName || `VND-${id}`;
  const getMed = (id) => medicines.find(m => m.medicineId === id);

  return (
    <div>
      <div className="view-header">
        <div>
          <span style={{ color: "var(--accent)", fontSize: "0.72rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "1.5px" }}>Configuration</span>
          <h2>Vendor–Medicine Links</h2>
          <p>Link a vendor to the medicines they supply. A vendor must be linked before purchase orders can be raised.</p>
        </div>
      </div>

      {/* Link form */}
      <div className="glass-card" style={{ marginBottom: "28px" }}>
        <h3 style={{ fontSize: "1rem", fontWeight: 700, marginBottom: "16px" }}>Create New Link</h3>
        <form onSubmit={handleLink} style={{ display: "flex", gap: "16px", alignItems: "flex-end", flexWrap: "wrap" }}>
          <div className="form-group" style={{ flex: 1, minWidth: "200px", margin: 0 }}>
            <label>Vendor</label>
            <select
              value={selectedVendor}
              onChange={e => setSelectedVendor(e.target.value)}
              required
              style={{ width: "100%" }}
            >
              <option value="">Select a vendor…</option>
              {vendors.map(v => (
                <option key={v.vendorId} value={v.vendorId}>
                  VND-{v.vendorId} — {v.vendorName}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group" style={{ flex: 1, minWidth: "200px", margin: 0 }}>
            <label>Medicine</label>
            <select
              value={selectedMedicine}
              onChange={e => setSelectedMedicine(e.target.value)}
              required
              style={{ width: "100%" }}
            >
              <option value="">Select a medicine…</option>
              {medicines.map(m => (
                <option key={m.medicineId} value={m.medicineId}>
                  {m.medicineCode} — {m.tradeName}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            className="btn-primary"
            disabled={submitting}
            style={{ whiteSpace: "nowrap", height: "40px" }}
          >
            <Link2 size={16} />
            {submitting ? "Linking…" : "Link Vendor"}
          </button>
        </form>
      </div>

      {/* Existing links table */}
      <h3 style={{ fontSize: "1rem", fontWeight: 700, marginBottom: "12px" }}>
        Existing Links
        {links.length > 0 && (
          <span className="badge info" style={{ marginLeft: 10, verticalAlign: "middle" }}>
            {links.length} total
          </span>
        )}
      </h3>
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Vendor</th>
              <th>Medicine Code</th>
              <th>Medicine Name</th>
              <th style={{ textAlign: "center" }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="4" style={{ textAlign: "center", padding: "40px" }}>Loading…</td></tr>
            ) : links.length === 0 ? (
              <tr><td colSpan="4" style={{ textAlign: "center", padding: "40px" }}>No links yet. Use the form above to create one.</td></tr>
            ) : links.map((link, idx) => {
              const med = getMed(link.medicineId);
              return (
                <tr key={idx}>
                  <td style={{ fontWeight: 600 }}>VND-{link.vendorId} — {getVendorName(link.vendorId)}</td>
                  <td style={{ fontFamily: "monospace", color: "var(--accent)" }}>{med?.medicineCode || `MED-${link.medicineId}`}</td>
                  <td>{med?.tradeName || `Medicine ${link.medicineId}`}</td>
                  <td style={{ textAlign: "center" }}>
                    <CheckCircle2 size={18} color="var(--success, #22c55e)" />
                  </td>
                </tr>
              );
            })}
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