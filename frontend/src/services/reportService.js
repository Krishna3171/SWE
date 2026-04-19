import { getAuthHeaders } from './authService';

const API_BASE_URL = "http://localhost:8080/api";

export const getProfitReport = async (startDate, endDate) => {
  let url = `${API_BASE_URL}/reports/profit`;
  const params = new URLSearchParams();
  if (startDate) params.append("startDate", startDate);
  if (endDate) params.append("endDate", endDate);

  const queryStr = params.toString();
  if (queryStr) {
    url += `?${queryStr}`;
  }

  const response = await fetch(url, {
    method: "GET",
    headers: getAuthHeaders()
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to fetch profit report");
  }
  return response.json();
};
