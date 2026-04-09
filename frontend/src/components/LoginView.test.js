import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import LoginView from "./LoginView";

describe("LoginView", () => {
  const mockOnLogin = jest.fn();
  const defaultProps = {
    onLogin: mockOnLogin,
    errorMessage: "",
    isSubmitting: false,
  };

  beforeEach(() => {
    mockOnLogin.mockClear();
  });

  test("renders login form with all elements", () => {
    render(<LoginView {...defaultProps} />);

    expect(screen.getByText("MSA Portal")).toBeInTheDocument();
    expect(screen.getByText("Sign in as Admin or Cashier to continue.")).toBeInTheDocument();
    expect(screen.getByLabelText("Role")).toBeInTheDocument();
    expect(screen.getByLabelText("Username")).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
  });

  test("submits form with correct data", async () => {
    render(<LoginView {...defaultProps} />);

    fireEvent.change(screen.getByLabelText("Username"), {
      target: { value: "testuser" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "password123" },
    });
    fireEvent.change(screen.getByLabelText("Role"), {
      target: { value: "cashier" },
    });

    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(mockOnLogin).toHaveBeenCalledWith({
        username: "testuser",
        password: "password123",
        role: "cashier",
      });
    });
  });

  test("displays error message when provided", () => {
    render(<LoginView {...defaultProps} errorMessage="Invalid credentials" />);

    expect(screen.getByText("Invalid credentials")).toBeInTheDocument();
  });

  test("disables form when submitting", () => {
    render(<LoginView {...defaultProps} isSubmitting={true} />);

    expect(screen.getByLabelText("Role")).toBeDisabled();
    expect(screen.getByLabelText("Username")).toBeDisabled();
    expect(screen.getByLabelText("Password")).toBeDisabled();
    expect(screen.getByRole("button", { name: /sign in/i })).toBeDisabled();
  });

  test("trims and lowercases username", async () => {
    render(<LoginView {...defaultProps} />);

    fireEvent.change(screen.getByLabelText("Username"), {
      target: { value: "  TestUser  " },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "password" },
    });

    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(mockOnLogin).toHaveBeenCalledWith({
        username: "testuser",
        password: "password",
        role: "admin",
      });
    });
  });
});