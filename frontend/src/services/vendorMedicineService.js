import { getAuthHeaders } from './authService';

const API_BASE = "http://localhost:8080/api";

export const getVendorMedicineLinks = async () => {
  const response = await fetch(`${API_BASE}/vendor-medicine`, {
    headers: getAuthHeaders()
  });
  if (!response.ok) throw new Error('Failed to fetch vendor-medicine links');
  const data = await response.json();
  return data.links || [];
};

export const linkVendorToMedicine = async (vendorId, medicineId) => {
  const response = await fetch(`${API_BASE}/vendor-medicine`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify({ vendorId, medicineId })
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'Failed to link vendor to medicine');
  return data;
};