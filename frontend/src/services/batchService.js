import { getAuthHeaders } from './authService';

const API_BASE = "http://localhost:8080/api";

export const getExpiredBatches = async () => {
  const response = await fetch(`${API_BASE}/batches/expired`, {
    headers: getAuthHeaders()
  });
  if (!response.ok) {
    throw new Error('Failed to fetch expired batches');
  }
  return response.json();
};

export const dropBatch = async (batchId) => {
  const response = await fetch(`${API_BASE}/batches/${batchId}`, {
    method: 'DELETE',
    headers: getAuthHeaders()
  });
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.error || 'Failed to drop batch');
  }
  return response.json();
};
