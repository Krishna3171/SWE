import { getAuthHeaders } from "./authService";

const API_BASE = "http://localhost:8080/api/users";

const buildHeaders = () => ({
  "Content-Type": "application/json",
  ...getAuthHeaders(),
});

const readErrorMessage = async (response, fallbackMessage) => {
  try {
    const payload = await response.json();
    return payload.error || fallbackMessage;
  } catch (error) {
    return fallbackMessage;
  }
};

export const getAllUsers = async () => {
  const response = await fetch(API_BASE, {
    method: "GET",
    headers: buildHeaders(),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, "Failed to fetch users"));
  }

  return response.json();
};

export const createUser = async (userData) => {
  const response = await fetch(API_BASE, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify(userData),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, "Failed to create user"));
  }

  return response.json();
};

export const updateUser = async (userId, userData) => {
  const response = await fetch(`${API_BASE}/${userId}`, {
    method: "PUT",
    headers: buildHeaders(),
    body: JSON.stringify(userData),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, "Failed to update user"));
  }

  return response.json();
};

export const deleteUser = async (userId) => {
  const response = await fetch(`${API_BASE}/${userId}`, {
    method: "DELETE",
    headers: buildHeaders(),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, "Failed to delete user"));
  }

  return response.json();
};
