function DashboardView({ user, onLogout }) {
  return (
    <main className="dashboard-shell">
      <header className="dashboard-header">
        <div>
          <h1>Dashboard</h1>
          <p>{user.role.toUpperCase()} access is ready. Feature modules are next.</p>
        </div>

        <div className="user-panel">
          <span>{user.displayName}</span>
          <button onClick={onLogout} type="button">
            Logout
          </button>
        </div>
      </header>
    </main>
  );
}

export default DashboardView;
