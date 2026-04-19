import "./App.css";
import { useState } from "react";
import DashboardView from "./components/DashboardView";
import LoginView from "./components/LoginView";
import { authenticateUser, clearAuth } from "./services/authService";

const APP_VIEW = {
  LOGIN: "login",
  DASHBOARD: "dashboard",
};

function App() {
  const [currentView, setCurrentView] = useState(APP_VIEW.LOGIN);
  const [currentUser, setCurrentUser] = useState(null);
  const [authError, setAuthError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogin = async (credentials) => {
    setIsSubmitting(true);
    setAuthError("");

    try {
      const user = await authenticateUser(credentials);
      setCurrentUser(user);
      setCurrentView(APP_VIEW.DASHBOARD);
    } catch (error) {
      setAuthError(error.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = () => {
    clearAuth();
    setCurrentUser(null);
    setCurrentView(APP_VIEW.LOGIN);
    setAuthError("");
  };

  if (currentView === APP_VIEW.LOGIN) {
    return (
      <LoginView
        onLogin={handleLogin}
        errorMessage={authError}
        isSubmitting={isSubmitting}
      />
    );
  }

  if (!currentUser) {
    return null;
  }

  return <DashboardView user={currentUser} onLogout={handleLogout} />;
}

export default App;
