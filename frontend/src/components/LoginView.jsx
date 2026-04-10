import { useState } from "react";

const INITIAL_FORM_STATE = {
  username: "",
  password: "",
  role: "admin",
};

function LoginView({ onLogin, errorMessage, isSubmitting }) {
  const [form, setForm] = useState(INITIAL_FORM_STATE);

  const onInputChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    await onLogin({
      username: form.username.trim().toLowerCase(),
      password: form.password,
      role: form.role,
    });
  };

  return (
    <main className="msa-shell">
      <section className="login-panel">
        <h1>MSA Portal</h1>
        <p className="subtitle">Sign in as Admin or Cashier to continue.</p>

        <form className="login-form" onSubmit={onSubmit}>
          <label htmlFor="role">Role</label>
          <select
            id="role"
            name="role"
            value={form.role}
            onChange={onInputChange}
            disabled={isSubmitting}
          >
            <option value="admin">Admin</option>
            <option value="cashier">Cashier</option>
          </select>

          <label htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            type="text"
            value={form.username}
            onChange={onInputChange}
            placeholder="Enter username"
            disabled={isSubmitting}
            required
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            value={form.password}
            onChange={onInputChange}
            placeholder="Enter password"
            disabled={isSubmitting}
            required
          />

          {errorMessage ? <p className="error-text">{errorMessage}</p> : null}

          <button type="submit" disabled={isSubmitting} aria-label="Sign In">
            {isSubmitting ? "Signing In..." : "Sign In"}
          </button>
        </form>
      </section>
    </main>
  );
}

export default LoginView;
