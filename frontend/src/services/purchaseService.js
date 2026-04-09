const API_BASE = "http://localhost:8080/api";

export const processPurchase = async (vendorId, items) => {
  const response = await fetch(`${API_BASE}/purchases`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ vendorId, items })
  });
  
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error || 'Failed to process supply purchase');
  }
  return response.json();
};
