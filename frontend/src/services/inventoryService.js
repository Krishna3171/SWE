const API_BASE = "http://localhost:8080/api";

export const getInventory = async () => {
  const response = await fetch(`${API_BASE}/inventory`);
  if (!response.ok) {
    throw new Error('Failed to fetch inventory');
  }
  return response.json();
};

export const getLowStock = async () => {
  const response = await fetch(`${API_BASE}/inventory/low-stock`);
  if (!response.ok) {
    throw new Error('Failed to fetch low stock');
  }
  return response.json();
};
