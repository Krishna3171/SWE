import { useState, useEffect } from "react";
import { getProfitReport } from "../services/reportService";
import { Calendar, DollarSign, Printer } from "lucide-react";

export default function ReportsAnalytics({ user }) {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState({ start: "2024-01-01", end: new Date().toISOString().split('T')[0] });

  const fetchReport = async () => {
    setLoading(true);
    try {
      const data = await getProfitReport(dateRange.start, dateRange.end);
      setReport(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user.role === "admin") {
      fetchReport();
    }
  }, [user.role]); // Note: In reality might want to trigger fetch manually on date change

  const executePrint = () => {
    window.print();
  };

  if (user.role !== "admin") {
    return (
      <div className="glass-card" style={{ textAlign: "center", padding: "80px 40px" }}>
        <h2 style={{ color: "var(--danger)", fontSize: "2rem" }}>Restricted Access</h2>
        <p style={{ fontSize: "1.1rem", color: "var(--text-secondary)" }}>You do not have administrative privileges to view financial analytics.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="view-header no-print">
        <div>
          <h2>Financial Analytics</h2>
          <p>Comprehensive profit and loss reporting.</p>
        </div>
        <button className="btn-secondary" onClick={executePrint}>
          <Printer size={18} /> Print Report
        </button>
      </div>

      <div className="glass-card no-print" style={{ marginBottom: "32px" }}>
        <div style={{ display: "flex", gap: "20px", alignItems: "flex-end" }}>
          <div className="form-group" style={{ flexGrow: 1 }}>
            <label>Start Date</label>
            <input type="date" value={dateRange.start} onChange={e => setDateRange({...dateRange, start: e.target.value})} />
          </div>
          <div className="form-group" style={{ flexGrow: 1 }}>
            <label>End Date</label>
            <input type="date" value={dateRange.end} onChange={e => setDateRange({...dateRange, end: e.target.value})} />
          </div>
          <button className="btn-primary" onClick={fetchReport}>
            <Calendar size={18} /> Generate Report
          </button>
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: "center", padding: "60px", color: "var(--text-secondary)" }}>Processing Analytics...</div>
      ) : report ? (
        <>
          <div className="stats-grid">
            <div className="stat-card glass-card info">
              <div className="stat-icon"><DollarSign size={24} /></div>
              <div>
                <h3>Gross Revenue</h3>
                <div className="value">${report.totalSalesRevenue?.toFixed(2) || '0.00'}</div>
              </div>
            </div>
            <div className="stat-card glass-card warning">
              <div className="stat-icon"><DollarSign size={24} /></div>
              <div>
                <h3>COGS (Purchase Costs)</h3>
                <div className="value">${report.totalPurchaseCost?.toFixed(2) || '0.00'}</div>
              </div>
            </div>
            <div className="stat-card glass-card success">
              <div className="stat-icon"><DollarSign size={24} /></div>
              <div>
                <h3>Net Profit</h3>
                <div className="value">${report.totalProfit?.toFixed(2) || '0.00'}</div>
              </div>
            </div>
            <div className="stat-card glass-card success">
              <div className="stat-icon"><DollarSign size={24} /></div>
              <div>
                <h3>Profit Margin</h3>
                <div className="value">{report.profitMargin?.toFixed(1) || '0'}%</div>
              </div>
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1.5fr 1fr", gap: "24px" }}>
            <div className="glass-card" style={{ padding: 0, overflow: "hidden" }}>
              <h3 style={{ padding: "20px 24px", margin: 0, borderBottom: "1px solid rgba(255,255,255,0.05)" }}>Product Performance</h3>
              <div className="table-container" style={{ border: "none" }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Sold</th>
                      <th>Revenue</th>
                      <th>Margin</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.medicineProfits?.map((m) => (
                      <tr key={m.medicineId}>
                        <td>{m.medicineName}</td>
                        <td>{m.totalQuantity}</td>
                        <td style={{color: "var(--success)"}}>${m.totalRevenue}</td>
                        <td><span className="badge success">{m.profitMargin.toFixed(1)}%</span></td>
                      </tr>
                    ))}
                    {(!report.medicineProfits || report.medicineProfits.length === 0) && (
                      <tr><td colSpan="4" style={{ textAlign: "center", padding: "20px" }}>No data points for this period.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Vendor Payment Summary Card */}
            <div className="glass-card">
              <h3 style={{ marginTop: 0 }}>Vendor Payment Outflows</h3>
              {report.vendorProfits?.map(v => (
                <div key={v.vendorId} style={{ display: "flex", justifyContent: "space-between", padding: "16px 0", borderBottom: "1px solid rgba(255,255,255,0.08)" }}>
                  <span style={{ fontWeight: 500 }}>{v.vendorName}</span>
                  <span style={{ color: "var(--warning)", fontWeight: 700 }}>${v.totalPurchaseCost?.toFixed(2) || '0.00'}</span>
                </div>
              ))}
              {(!report.vendorProfits || report.vendorProfits.length === 0) && (
                <p style={{ color: "var(--text-secondary)" }}>No purchases recorded.</p>
              )}
            </div>
          </div>
          
          {/* Print specific structure */}
          <div className="printable-area">
            <h2>MSA Financial Report</h2>
            <p><strong>Period:</strong> {dateRange.start} TO {dateRange.end}</p>
            <hr />
            <p><strong>Gross Revenue:</strong> ${report.totalSalesRevenue}</p>
            <p><strong>Total Purchase Cost:</strong> ${report.totalPurchaseCost}</p>
            <p><strong>Net Profit:</strong> ${report.totalProfit}</p>
            <p><strong>Profit Margin:</strong> {report.profitMargin}%</p>
          </div>
        </>
      ) : (
        <p>No report available.</p>
      )}
    </div>
  );
}
