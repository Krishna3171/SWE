const API_BASE = "http://localhost:8080/api";

export const getExpiredBatches = async () => {
  const response = await fetch(`${API_BASE}/batches/expired`);
  if (!response.ok) {
    throw new Error('Failed to fetch expired batches');
  }
  return response.json();
};
