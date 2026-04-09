import { authenticateUser } from "./authService";

// Mock fetch globally
global.fetch = jest.fn();

describe("authenticateUser", () => {
  beforeEach(() => {
    fetch.mockClear();
  });

  test("successfully authenticates user", async () => {
    const mockResponse = {
      username: "testuser",
      role: "admin",
      displayName: "Test User",
    };

    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    });

    const credentials = {
      username: "testuser",
      password: "password123",
      role: "admin",
    };

    const result = await authenticateUser(credentials);

    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/api/users/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(credentials),
    });

    expect(result).toEqual({
      username: "testuser",
      role: "admin",
      displayName: "Test User",
    });
  });

  test("handles 401 unauthorized", async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: () => Promise.resolve({ error: "Invalid credentials" }),
    });

    const credentials = {
      username: "testuser",
      password: "wrongpassword",
      role: "admin",
    };

    await expect(authenticateUser(credentials)).rejects.toThrow(
      "Invalid credentials for the selected role."
    );
  });

  test("handles server error", async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: () => Promise.resolve({ error: "Server error" }),
    });

    const credentials = {
      username: "testuser",
      password: "password123",
      role: "admin",
    };

    await expect(authenticateUser(credentials)).rejects.toThrow("Server error");
  });

  test("handles network error", async () => {
    fetch.mockRejectedValueOnce(new Error("Network error"));

    const credentials = {
      username: "testuser",
      password: "password123",
      role: "admin",
    };

    await expect(authenticateUser(credentials)).rejects.toThrow(
      "Login service is unavailable. Ensure backend server is running and reachable."
    );
  });

  test("maps backend user correctly when displayName is missing", async () => {
    const mockResponse = {
      username: "testuser",
      role: "cashier",
    };

    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    });

    const credentials = {
      username: "testuser",
      password: "password123",
      role: "cashier",
    };

    const result = await authenticateUser(credentials);

    expect(result).toEqual({
      username: "testuser",
      role: "cashier",
      displayName: "MSA User",
    });
  });
});