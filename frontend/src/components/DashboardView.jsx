import { useState, useEffect } from "react";
import { 
  LayoutDashboard, 
  Pill, 
  Warehouse, 
  Users, 
  ClipboardList, 
  ShoppingCart, 
  AlertTriangle, 
  BarChart3, 
  LogOut,
  Search,
  Bell,
  Settings
} from "lucide-react";

import DashboardHome from "./DashboardHome";
import MedicineManagement from "./MedicineManagement";
import InventoryStock from "./InventoryStock";
import VendorManagement from "./VendorManagement";
import OrderGeneration from "./OrderGeneration";
import SalesBilling from "./SalesBilling";
import ExpiredMedicines from "./ExpiredMedicines";
import ReportsAnalytics from "./ReportsAnalytics";

export default function DashboardView({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState("dashboard");
  const [currentTime, setCurrentTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const renderContent = () => {
    switch (activeTab) {
      case "dashboard": return <DashboardHome />;
      case "medicine": return <MedicineManagement />;
      case "inventory": return <InventoryStock />;
      case "vendors": return <VendorManagement />;
      case "orders": return <OrderGeneration />;
      case "sales": return <SalesBilling />;
      case "expired": return <ExpiredMedicines />;
      case "reports": return <ReportsAnalytics user={user} />;
      default: return <DashboardHome />;
    }
  };

  const NavButton = ({ id, label, Icon }) => (
    <button 
      className={`nav-item ${activeTab === id ? "active" : ""}`}
      onClick={() => setActiveTab(id)}
    >
      <Icon size={17} />
      {label}
    </button>
  );

  const dateStr = currentTime.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).toUpperCase();
  const timeStr = currentTime.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="dashboard-layout">
      {/* Sidebar */}
      <aside className="dashboard-sidebar no-print">
        <div className="sidebar-header">
          <span className="brand">MSA System</span>
          <span className="sidebar-subtitle">Clinical Luminary</span>
        </div>
        
        <nav className="nav-links">
          <NavButton id="dashboard" label="Dashboard" Icon={LayoutDashboard} />
          <NavButton id="medicine" label="Medicines" Icon={Pill} />
          <NavButton id="inventory" label="Inventory & Stock" Icon={Warehouse} />
          <NavButton id="orders" label="Orders" Icon={ClipboardList} />
          <NavButton id="sales" label="Sales/POS" Icon={ShoppingCart} />
          
          {user.role === "admin" && (
            <>
              <NavButton id="vendors" label="Vendors" Icon={Users} />
              <NavButton id="expired" label="Expired Alerts" Icon={AlertTriangle} />
              <NavButton id="reports" label="Reports" Icon={BarChart3} />
            </>
          )}
        </nav>

        <div className="user-snippet">
          <div>
            <strong>{user.displayName}</strong>
            <br />
            <span>{user.role === "admin" ? "Systems Head" : "Active Session"}</span>
          </div>
          <button className="logout-btn" onClick={onLogout}>
            <LogOut size={16} /> Logout
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="main-wrapper">
        <header className="top-navbar no-print">
          <div className="top-nav-search">
            <Search size={16} className="search-icon" />
            <input type="text" placeholder="Search medicines, codes, or generic names..." />
          </div>
          <div className="top-nav-time">
            {dateStr} • {timeStr}
          </div>
          <div className="top-nav-profile">
            <div className="top-nav-icons">
              <button><Bell size={18} /></button>
              <button><Settings size={18} /></button>
            </div>
          </div>
        </header>

        <main className="dashboard-content">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}
