import { useEffect, useState } from "react";
import { Plus, Search, X, Pencil, Trash2, Users as UsersIcon } from "lucide-react";
import { createUser, deleteUser, getAllUsers, updateUser } from "../services/userService";

const INITIAL_FORM = {
  username: "",
  password: "",
  role: "cashier",
};

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [toast, setToast] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUserId, setEditingUserId] = useState(null);
  const [form, setForm] = useState(INITIAL_FORM);

  const fetchUsers = async () => {
    try {
      const data = await getAllUsers();
      setUsers(data);
    } catch (error) {
      setToast({ type: "error", msg: error.message });
      setTimeout(() => setToast(null), 3000);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const openCreate = () => {
    setEditingUserId(null);
    setForm(INITIAL_FORM);
    setIsModalOpen(true);
  };

  const openEdit = (user) => {
    setEditingUserId(user.userId);
    setForm({ username: user.username, password: "", role: user.role || "cashier" });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingUserId(null);
    setForm(INITIAL_FORM);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const payload = {
      username: form.username.trim().toLowerCase(),
      password: form.password,
      role: form.role,
    };

    try {
      if (editingUserId) {
        await updateUser(editingUserId, payload);
        setToast({ type: "success", msg: "User updated successfully." });
      } else {
        await createUser(payload);
        setToast({ type: "success", msg: "User created successfully." });
      }

      setTimeout(() => setToast(null), 3000);
      closeModal();
      setLoading(true);
      await fetchUsers();
    } catch (error) {
      setToast({ type: "error", msg: error.message });
      setTimeout(() => setToast(null), 3000);
    }
  };

  const handleDelete = async (userId, username) => {
    const shouldDelete = window.confirm(`Delete user ${username}?`);
    if (!shouldDelete) {
      return;
    }

    try {
      await deleteUser(userId);
      setToast({ type: "success", msg: "User deleted successfully." });
      setTimeout(() => setToast(null), 3000);
      setUsers((prev) => prev.filter((item) => item.userId !== userId));
    } catch (error) {
      setToast({ type: "error", msg: error.message });
      setTimeout(() => setToast(null), 3000);
    }
  };

  const filteredUsers = users.filter((user) => {
    const query = search.toLowerCase();
    return user.username.toLowerCase().includes(query) || user.role.toLowerCase().includes(query);
  });

  return (
    <div>
      <div className="view-header">
        <div>
          <h2>User Management</h2>
          <p>Create, update, and remove platform users. Admin role is required for all write operations.</p>
        </div>
        <button className="btn-primary" onClick={openCreate}>
          <Plus size={16} /> Add User
        </button>
      </div>

      <h3 style={{ fontSize: "1.1rem", fontWeight: 700, margin: "0 0 16px" }}>User Directory</h3>

      <div className="search-wrapper">
        <Search size={16} className="search-icon" />
        <input
          type="text"
          placeholder="Search by username or role..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>User ID</th>
              <th>Username</th>
              <th>Role</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan="4" style={{ textAlign: "center" }}>Loading...</td></tr> : null}
            {!loading && filteredUsers.map((user) => (
              <tr key={user.userId}>
                <td style={{ fontFamily: "monospace", color: "var(--accent)" }}>USR-{user.userId}</td>
                <td style={{ fontWeight: 600 }}>{user.username}</td>
                <td>
                  <span className={`badge ${user.role === "admin" ? "warning" : "success"}`}>
                    {user.role}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", gap: "8px" }}>
                    <button className="btn-secondary" onClick={() => openEdit(user)}>
                      <Pencil size={14} /> Edit
                    </button>
                    <button className="btn-secondary" onClick={() => handleDelete(user.userId, user.username)}>
                      <Trash2 size={14} /> Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {!loading && filteredUsers.length === 0 ? (
              <tr>
                <td colSpan="4" style={{ textAlign: "center", padding: "40px" }}>
                  <UsersIcon size={20} style={{ verticalAlign: "middle", marginRight: "8px" }} />
                  No users found.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>

      {isModalOpen ? (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>{editingUserId ? "Edit User" : "Create User"}</h3>
              <button className="close-btn" onClick={closeModal}><X size={24} /></button>
            </div>

            <form className="dash-form" onSubmit={handleSubmit}>
              <div className="form-group full-width">
                <label htmlFor="username">Username</label>
                <input
                  id="username"
                  value={form.username}
                  onChange={(event) => setForm((prev) => ({ ...prev, username: event.target.value }))}
                  required
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="password">Password</label>
                <input
                  id="password"
                  type="password"
                  value={form.password}
                  onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
                  placeholder={editingUserId ? "Enter new password" : "Enter password"}
                  required
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="role">Role</label>
                <select
                  id="role"
                  value={form.role}
                  onChange={(event) => setForm((prev) => ({ ...prev, role: event.target.value }))}
                  required
                >
                  <option value="admin">admin</option>
                  <option value="cashier">cashier</option>
                </select>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>Cancel</button>
                <button type="submit" className="btn-primary">{editingUserId ? "Save Changes" : "Create User"}</button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {toast ? (
        <div className="toast-container no-print">
          <div className={`toast ${toast.type}`}>
            {toast.msg}
          </div>
        </div>
      ) : null}
    </div>
  );
}
