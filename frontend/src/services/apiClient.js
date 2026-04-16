const DEFAULT_BASE_URL =
  process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

const isAbsoluteUrl = (value) => /^https?:\/\//i.test(value);

const buildUrl = (path) => {
  if (!path) {
    return DEFAULT_BASE_URL;
  }

  if (isAbsoluteUrl(path)) {
    return path;
  }

  return `${DEFAULT_BASE_URL}${path}`;
};

const parseResponseBody = async (response) => {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
};

const buildErrorMessage = async (response) => {
  const body = await parseResponseBody(response);

  if (body && typeof body === "object" && body.error) {
    return body.error;
  }

  if (typeof body === "string" && body.trim()) {
    return body;
  }

  return `Request failed with status ${response.status}`;
};

const request = async (method, path, body) => {
  const options = {
    method,
    headers: {},
  };

  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = typeof body === "string" ? body : JSON.stringify(body);
  }

  const response = await fetch(buildUrl(path), options);

  if (!response.ok) {
    throw new Error(await buildErrorMessage(response));
  }

  return parseResponseBody(response);
};

export const getJson = (path) => request("GET", path);

export const postJson = (path, body) => request("POST", path, body);

export const putJson = (path, body) => request("PUT", path, body);

export const deleteJson = (path, body) => request("DELETE", path, body);

export const API_BASE_URL = DEFAULT_BASE_URL;
