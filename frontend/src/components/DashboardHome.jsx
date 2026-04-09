import { useState, useEffect } from "react";
import { Package, AlertTriangle, DollarSign, TrendingUp } from "lucide-react";
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { getAllMedicines } from "../services/medicineService";
import { getLowStock } from "../services/inventoryService";
import { getProfitReport } from "../services/reportService";

const chartData = [
  { name: 'MON', revenue: 400 },
  { name: 'TUE', revenue: 300 },
  { name: 'WED', revenue: 550 },
  { name: 'THU', revenue: 1142 },
  { name: 'FRI', revenue: 700 },
  { name: 'SAT', revenue: 900 },
  { name: 'SUN', revenue: 650 },
];

export default function DashboardHome() {
  const [stats, setStats] = useState({ totalMeds: 0, lowStock: 0, revenue: 0, profit: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchGlobalStats = async () => {
      try {
        const meds = await getAllMedicines();
        const lowStock = await getLowStock();
        // Use all-time data instead of today-only to get accurate P&L
        let report = null;
        try { report = await getProfitReport(null, null); } catch (e) {}

        setStats({
          totalMeds: meds.length,
          lowStock: lowStock.length,
          revenue: report ? report.totalSalesRevenue : 0,
          profit: report ? report.totalProfit : 0
        });
      } catch (e) { console.error(e); }
      finally { setLoading(false); }
    };
    fetchGlobalStats();
  }, []);

  const profitNegative = stats.profit < 0;

  return (
    <div>
      <div className="view-header">
        <div>
          <h2>Good morning, Administrator</h2>
          <p>Here is the status of your pharmacy operations.</p>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card glass-card accent">
          <div className="stat-label-row">
            <div className="stat-icon"><Package size={16} /></div>
            <h3>Inventory</h3>
          </div>
          <div className="value">{loading ? "..." : stats.totalMeds}</div>
          <span style={{ color: "var(--text-secondary)", fontSize: "0.8rem" }}>Total Medicines</span>
        </div>
        
        <div className="stat-card glass-card danger">
          <div className="stat-label-row">
            <div className="stat-icon"><AlertTriangle size={16} /></div>
            <h3>Critical</h3>
          </div>
          <div className="value">{loading ? "..." : stats.lowStock}</div>
          <span style={{ color: "var(--text-secondary)", fontSize: "0.8rem" }}>Low Stock Items</span>
        </div>
        
        <div className="stat-card glass-card success">
          <div className="stat-label-row">
            <div className="stat-icon"><DollarSign size={16} /></div>
            <h3>Revenue</h3>
          </div>
          <div className="value">{loading ? "..." : `$${stats.revenue?.toFixed(2)}`}</div>
          <span style={{ color: "var(--text-secondary)", fontSize: "0.8rem" }}>All-Time Total</span>
        </div>
        
        <div className="stat-card glass-card warning">
          <div className="stat-label-row">
            <div className="stat-icon"><TrendingUp size={16} /></div>
            <h3>P&L Forecast</h3>
          </div>
          <div className="value" style={{ color: profitNegative ? "var(--danger)" : "var(--text-primary)" }}>{loading ? "..." : `$${stats.profit?.toFixed(2)}`}</div>
          <span style={{ color: "var(--text-secondary)", fontSize: "0.8rem" }}>Net Profit (All-Time)</span>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1.6fr 1fr", gap: "20px" }}>
        <div className="glass-card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
            <div>
              <h3 style={{ margin: 0, fontSize: "1.1rem", fontWeight: 700 }}>Weekly Revenue Trend</h3>
              <span style={{ color: "var(--text-secondary)", fontSize: "0.82rem" }}>Growth across all pharmacy branches</span>
            </div>
          </div>
          <div style={{ width: '100%', height: 280 }}>
            <ResponsiveContainer>
              <AreaChart data={chartData} margin={{ top: 10, right: 20, left: -10, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#00D4C8" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#00D4C8" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="name" stroke="#6B7A99" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="#6B7A99" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => `$${v}`} />
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" vertical={false} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#161A30', borderColor: '#00D4C8', borderRadius: '8px', border: '1px solid rgba(0,212,200,0.2)' }} 
                  itemStyle={{ color: '#E8ECF4' }}
                  labelStyle={{ color: '#6B7A99' }}
                />
                <Area type="monotone" dataKey="revenue" stroke="#00D4C8" strokeWidth={2.5} fillOpacity={1} fill="url(#colorRev)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="glass-card">
          <h3 style={{ margin: "0 0 16px", fontSize: "1.1rem", fontWeight: 700 }}>Recent Sales</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "0.82rem", marginTop: "-10px", marginBottom: "16px" }}>Real-time transaction log from POS terminals</p>
          <div className="table-container" style={{ border: "none", background: "transparent" }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Amount</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr><td>Today</td><td style={{fontWeight:700}}>${stats.revenue?.toFixed(2) || "0.00"}</td><td><span className="badge success">Paid</span></td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
