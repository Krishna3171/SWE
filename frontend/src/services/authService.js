import { postJson } from "./apiClient";

export const authenticateUser = async (credentials) => {
  const payload = await postJson("/api/users/login", credentials);

  return {
    username: payload.username || credentials.username,
    role: payload.role || credentials.role,
    displayName:
      payload.displayName || payload.fullName || payload.name || "MSA User",
  };
};
