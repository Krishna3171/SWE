import { render, screen } from "@testing-library/react";
import App from "./App";

test("renders login portal heading", () => {
  render(<App />);
  const headingElement = screen.getByText(/msa portal/i);
  expect(headingElement).toBeInTheDocument();
});
