import { useState, useEffect } from "react";
import { Search, PackageOpen } from "lucide-react";
import { getInventory } from "../services/inventoryService";

export default function InventoryStock() {
  const [stockInfo, setStockInfo] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

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

  const filteredStock = stockInfo.filter(item =>
    item.code.toLowerCase().includes(search.toLowerCase()) ||
    item.tradeName.toLowerCase().includes(search.toLowerCase())
  );

  const lowStockCount = stockInfo.filter(i => i.currentStock <= (i.dynamicThreshold ?? i.threshold)).length;
  const totalStock = stockInfo.reduce((s, i) => s + i.currentStock, 0);

  return (
    <div>
      <div className="view-header no-print">
        <div>
          <h2>Inventory & Stock</h2>
          <p>Manage and monitor your pharmaceutical supply levels with real-time analytics and stock tracking.</p>
        </div>
      </div>

      <div className="stats-grid no-print" style={{ gridTemplateColumns: "repeat(2, 1fr)" }}>
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
              const isLow = item.currentStock <= (item.dynamicThreshold ?? item.threshold);
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

    </div>
  );
}
