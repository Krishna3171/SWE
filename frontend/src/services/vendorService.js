import { getAuthHeaders } from './authService';

const API_BASE = "http://localhost:8080/api";

export const getAllVendors = async () => {
  const response = await fetch(`${API_BASE}/vendors`);
  if (!response.ok) {
    throw new Error('Failed to fetch vendors');
  }
  return response.json();
};

export const createVendor = async (vendorData) => {
  const response = await fetch(`${API_BASE}/vendors`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(vendorData)
  });
  
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error || 'Failed to create vendor');
  }
  return response.json();
};

export const updateVendor = async (vendorData) => {
  const response = await fetch(`${API_BASE}/vendors`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(vendorData)
  });
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.error || 'Failed to update vendor');
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
};
