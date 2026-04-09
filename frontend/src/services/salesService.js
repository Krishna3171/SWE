const API_BASE_URL = "http://localhost:8080/api";

export const makeSale = async (items) => {
  const response = await fetch(`${API_BASE_URL}/sales`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ items }),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to process sale");
  }
  return response.json();
};

export const getRecentSales = async () => {
  const response = await fetch(`${API_BASE_URL}/sales`, {
    method: "GET",
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to fetch recent sales");
  }
  return response.json();
};

