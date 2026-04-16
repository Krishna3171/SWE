function prettyPrint(value) {
  if (value === null || value === undefined) {
    return "";
  }

  if (typeof value === "string") {
    return value;
  }

  return JSON.stringify(value, null, 2);
}

function ResponsePanel({ title, payload, error }) {
  const hasError = Boolean(error);
  const hasPayload = payload !== null && payload !== undefined;

  return (
    <section className="result-panel" aria-live="polite">
      <div className="result-header">
        <h3>Response</h3>
        <span>{title || "No action yet"}</span>
      </div>
      {hasError ? (
        <pre className="result-error">{error}</pre>
      ) : (
        <pre className="result-json">
          {hasPayload ? prettyPrint(payload) : "Run an action to view the backend response."}
        </pre>
      )}
    </section>
  );
}

export default ResponsePanel;