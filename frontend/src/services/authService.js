const USER_CONTROLLER_LOGIN_URL =
  process.env.REACT_APP_USER_LOGIN_URL ||
  "http://localhost:8080/api/users/login";

export const getAuthHeaders = () => {
  const role = sessionStorage.getItem("userRole");
  return role ? { "X-User-Role": role } : {};
};

export const clearAuth = () => sessionStorage.removeItem("userRole");

const mapBackendUser = (backendUser, credentials) => ({
  username: backendUser.username || credentials.username,
  role: (backendUser.role || credentials.role).toLowerCase(),
  displayName:
    backendUser.displayName ||
    backendUser.fullName ||
    backendUser.name ||
    "MSA User",
});

export const authenticateUser = async (credentials) => {
  // UserController endpoint contract (fictional for now):
  // POST /api/users/login { username, password, role }
  // 200 -> { username, role, displayName }
  try {
    const response = await fetch(USER_CONTROLLER_LOGIN_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(credentials),
    });

    if (response.ok) {
      const payload = await response.json();
      const user = mapBackendUser(payload, credentials);
      sessionStorage.setItem("userRole", user.role);
      return user;
    }

    let backendError = "";
    try {
      const errorPayload = await response.json();
      backendError = errorPayload.error || "";
    } catch (parseError) {
      backendError = "";
    }

    if (response.status === 401) {
      throw new Error("Invalid credentials for the selected role.");
    }

    throw new Error(
      backendError ||
        "Login service is unavailable. Ensure backend server is running and reachable.",
    );
  } catch (error) {
    const message = typeof error?.message === "string" ? error.message : "";
    const looksLikeNetworkError = message.toLowerCase().includes("network");
    if (error instanceof TypeError || looksLikeNetworkError) {
      throw new Error(
        "Login service is unavailable. Ensure backend server is running and reachable.",
      );
    }

    throw error;
  }
};
