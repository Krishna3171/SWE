import { useState, useEffect } from "react";
import {
  Pill,
  Warehouse,
  Users,
  ClipboardList,
  ShoppingCart,
  AlertTriangle,
  BarChart3,
  LogOut,
  Link2,
} from "lucide-react";

import MedicineManagement from "./MedicineManagement";
import InventoryStock from "./InventoryStock";
import VendorManagement from "./VendorManagement";
import VendorMedicineLink from "./VendorMedicineLink";
import OrderGeneration from "./OrderGeneration";
import SalesBilling from "./SalesBilling";
import ExpiredMedicines from "./ExpiredMedicines";
import ReportsAnalytics from "./ReportsAnalytics";
import UserManagement from "./UserManagement";

const CASHIER_TABS = ["orders", "sales", "inventory"];
const ADMIN_TABS = ["medicine", "inventory", "orders", "sales", "vendors", "links", "users", "expired", "reports"];

export default function DashboardView({ user, onLogout }) {
  const allowedTabs = user.role === "admin" ? ADMIN_TABS : CASHIER_TABS;
  const [activeTab, setActiveTab] = useState(allowedTabs[0]);
  const [currentTime, setCurrentTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const safeTab = allowedTabs.includes(activeTab) ? activeTab : allowedTabs[0];

  const renderContent = () => {
    switch (safeTab) {
      case "medicine": return <MedicineManagement />;
      case "inventory": return <InventoryStock />;
      case "vendors": return <VendorManagement />;
      case "links": return <VendorMedicineLink />;
      case "users": return <UserManagement />;
      case "orders": return <OrderGeneration user={user} />;
      case "sales": return <SalesBilling />;
      case "expired": return <ExpiredMedicines />;
      case "reports": return <ReportsAnalytics user={user} />;
      default: return null;
    }
  };

  const NavButton = ({ id, label, Icon }) => (
    <button
      className={`nav-item ${safeTab === id ? "active" : ""}`}
      onClick={() => setActiveTab(id)}
    >
      <Icon size={17} />
      {label}
    </button>
  );

  const dateStr = currentTime.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" }).toUpperCase();
  const timeStr = currentTime.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" });

  return (
    <div className="dashboard-layout">
      {/* Sidebar */}
      <aside className="dashboard-sidebar no-print">
        <div className="sidebar-header">
          <span className="brand">MSA System</span>
          <span className="sidebar-subtitle">Clinical Luminary</span>
        </div>

        <nav className="nav-links">
          {user.role === "cashier" ? (
            <>
              <NavButton id="orders" label="Orders" Icon={ClipboardList} />
              <NavButton id="sales" label="Sales/POS" Icon={ShoppingCart} />
              <NavButton id="inventory" label="Inventory & Stock" Icon={Warehouse} />
            </>
          ) : (
            <>
              <NavButton id="medicine" label="Medicines" Icon={Pill} />
              <NavButton id="inventory" label="Inventory & Stock" Icon={Warehouse} />
              <NavButton id="orders" label="Orders" Icon={ClipboardList} />
              <NavButton id="sales" label="Sales/POS" Icon={ShoppingCart} />
              <NavButton id="vendors" label="Vendors" Icon={Users} />
              <NavButton id="links" label="Vendor-Medicine" Icon={Link2} />
              <NavButton id="users" label="User Accounts" Icon={Users} />
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
          <div className="top-nav-time">
            {dateStr} • {timeStr}
          </div>
        </header>

        <main className="dashboard-content">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}
