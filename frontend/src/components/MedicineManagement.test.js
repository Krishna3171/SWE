import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import MedicineManagement from "./MedicineManagement";
import { getAllMedicines, addMedicine } from "../services/medicineService";

// Mock the service
jest.mock("../services/medicineService");

describe("MedicineManagement", () => {
  const mockMedicines = [
    {
      medicineId: 1,
      medicineCode: "MED1",
      tradeName: "Aspirin",
      genericName: "Acetylsalicylic Acid",
      unitSellingPrice: 10.00,
      unitPurchasePrice: 8.00,
    },
    {
      medicineId: 2,
      medicineCode: "MED2",
      tradeName: "Paracetamol",
      genericName: "Paracetamol",
      unitSellingPrice: 5.00,
      unitPurchasePrice: 4.00,
    },
  ];

  beforeEach(() => {
    getAllMedicines.mockResolvedValue(mockMedicines);
    addMedicine.mockResolvedValue({});
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test("renders loading state initially", () => {
    // Keep fetch pending in this test so no async state update fires after assertion.
    getAllMedicines.mockImplementationOnce(() => new Promise(() => {}));
    render(<MedicineManagement />);
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  test("renders medicines list after loading", async () => {
    render(<MedicineManagement />);

    await waitFor(() => {
      expect(screen.getByText("Aspirin")).toBeInTheDocument();
      expect(screen.getByText("Paracetamol")).toBeInTheDocument();
    });

    expect(getAllMedicines).toHaveBeenCalledTimes(1);
  });

  test("filters medicines based on search", async () => {
    render(<MedicineManagement />);

    await waitFor(() => {
      expect(screen.getByText("Aspirin")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText("Search medicines..."), {
      target: { value: "paracetamol" },
    });

    expect(screen.queryByText("Aspirin")).not.toBeInTheDocument();
    expect(screen.getByText("Paracetamol")).toBeInTheDocument();
  });

  test("opens add medicine form", async () => {
    render(<MedicineManagement />);

    await waitFor(() => {
      expect(screen.getByText("Aspirin")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Add Medicine"));

    expect(screen.getByText("Add New Medicine")).toBeInTheDocument();
    expect(screen.getByLabelText("Trade Name")).toBeInTheDocument();
    expect(screen.getByLabelText("Generic Name")).toBeInTheDocument();
  });

  test("submits add medicine form successfully", async () => {
    getAllMedicines
      .mockResolvedValueOnce(mockMedicines)
      .mockResolvedValueOnce([
        ...mockMedicines,
        {
          medicineId: 3,
          medicineCode: "MED3",
          tradeName: "Ibuprofen",
          genericName: "Ibuprofen",
          unitSellingPrice: 15.0,
          unitPurchasePrice: 12.0,
        },
      ]);
    addMedicine.mockResolvedValue({ medicineCode: "MED3" });

    render(<MedicineManagement />);

    await waitFor(() => {
      expect(screen.getByText("Aspirin")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Add Medicine"));

    fireEvent.change(screen.getByLabelText("Trade Name"), {
      target: { value: "Ibuprofen" },
    });
    fireEvent.change(screen.getByLabelText("Generic Name"), {
      target: { value: "Ibuprofen" },
    });
    fireEvent.change(screen.getByLabelText("Unit Selling Price"), {
      target: { value: "15.00" },
    });
    fireEvent.change(screen.getByLabelText("Unit Purchase Price"), {
      target: { value: "12.00" },
    });
    fireEvent.change(screen.getByLabelText("Initial Quantity"), {
      target: { value: "100" },
    });
    fireEvent.change(screen.getByLabelText("Expiry Date"), {
      target: { value: "2025-12-31" },
    });
    fireEvent.change(screen.getByLabelText("Reorder Threshold"), {
      target: { value: "10" },
    });
    fireEvent.change(screen.getByLabelText("Vendor ID"), {
      target: { value: "1" },
    });

    fireEvent.click(screen.getByText("Add Medicine"));

    await waitFor(() => {
      expect(addMedicine).toHaveBeenCalledWith({
        tradeName: "Ibuprofen",
        genericName: "Ibuprofen",
        unitSellingPrice: "15.00",
        unitPurchasePrice: "12.00",
        initialQuantity: "100",
        expiryDate: "2025-12-31",
        reorderThreshold: "10",
        vendorId: "1",
      });
    });

    await waitFor(() => {
      expect(screen.getByText("Medicine Added Successfully!")).toBeInTheDocument();
    });
  });

  test("handles add medicine error", async () => {
    addMedicine.mockRejectedValue(new Error("Failed to add medicine"));
    
    // Suppress console.error so it doesn't pollute the test runner logs
    const consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});

    render(<MedicineManagement />);

    await waitFor(() => {
      expect(screen.getByText("Aspirin")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("Add Medicine"));

    fireEvent.change(screen.getByLabelText("Trade Name"), {
      target: { value: "Ibuprofen" },
    });
    fireEvent.change(screen.getByLabelText("Generic Name"), {
      target: { value: "Ibuprofen" },
    });
    fireEvent.change(screen.getByLabelText("Unit Selling Price"), {
      target: { value: "15.00" },
    });
    fireEvent.change(screen.getByLabelText("Unit Purchase Price"), {
      target: { value: "12.00" },
    });

    fireEvent.click(screen.getByText("Add Medicine"));

    await waitFor(() => {
      expect(screen.getByText("Failed to add medicine")).toBeInTheDocument();
    });

    consoleSpy.mockRestore();
  });
});