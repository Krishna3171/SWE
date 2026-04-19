import { getAuthHeaders } from './authService';

const API_BASE = "http://localhost:8080/api";

export const processPurchase = async (vendorId, items) => {
  const response = await fetch(`${API_BASE}/purchases`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify({ vendorId, items })
  });
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error || 'Failed to process supply purchase');
  }
  return response.json();
};

export const getPendingPurchases = async () => {
  const response = await fetch(`${API_BASE}/purchases/pending`, {
    headers: getAuthHeaders()
  });
  if (!response.ok) throw new Error('Failed to fetch purchase orders');
  return response.json();
};

export const receivePurchaseLine = async (purchaseId, batchId) => {
  const response = await fetch(`${API_BASE}/purchases/receive`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify({ purchaseId, batchId })
  });
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error || 'Failed to receive purchase line');
  }
  return response.json();
};
