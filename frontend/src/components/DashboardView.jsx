import AdminWorkspace from "./dashboard/AdminWorkspace";
import CashierWorkspace from "./dashboard/CashierWorkspace";
import { API_BASE_URL } from "../services/apiClient";

function DashboardView({ user, onLogout }) {
  const isAdmin = String(user?.role || "").toLowerCase() === "admin";

  return (
    <main className="dashboard-shell">
      <section className="hero-card">
        <div>
          <p className="eyebrow">Pharmacy operations console</p>
          <h1>MSA Control Room</h1>
          <p className="hero-copy">
            {isAdmin
              ? "Admin workspace for users, catalog, inventory, and reports."
              : "Cashier workspace for purchases, receiving, and sales."}
          </p>
        </div>

        <div className="user-panel">
          <span className="user-role">{user.role.toUpperCase()}</span>
          <strong>{user.displayName}</strong>
          <small>{API_BASE_URL}</small>
          <button onClick={onLogout} type="button">
            Logout
          </button>
        </div>
      </section>

      {isAdmin ? <AdminWorkspace user={user} /> : <CashierWorkspace user={user} />}
    </main>
  );
}

export default DashboardView;
