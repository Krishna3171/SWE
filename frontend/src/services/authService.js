const USER_CONTROLLER_LOGIN_URL =
  process.env.REACT_APP_USER_LOGIN_URL ||
  "http://localhost:8080/api/users/login";

const mapBackendUser = (backendUser, credentials) => ({
  username: backendUser.username || credentials.username,
  role: backendUser.role || credentials.role,
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
      return mapBackendUser(payload, credentials);
    }

    let backendError = "";
    try {
      const errorPayload = await response.json();
      backendError = errorPayload.error || "";
    } catch (parseError) {
      backendError = "";
    }

    if (response.status === 401) {
      throw new Error(
        backendError || "Invalid credentials for the selected role.",
      );
    }

    throw new Error(
      backendError ||
        "Login service is unavailable. Ensure backend server is running and reachable.",
    );
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error(
        "Cannot connect to backend. Start the backend server and verify the login API URL.",
      );
    }

    throw error;
  }
};
