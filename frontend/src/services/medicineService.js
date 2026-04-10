const API_BASE_URL = "http://localhost:8080/api";

export const getAllMedicines = async () => {
  const response = await fetch(`${API_BASE_URL}/medicines?t=${Date.now()}`, {
    method: "GET",
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error("Failed to fetch medicines");
  }
  return response.json();
};

export const addMedicine = async (medicineData) => {
  const response = await fetch(`${API_BASE_URL}/medicines`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(medicineData),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || "Failed to add medicine");
  }
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
};
