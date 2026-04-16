function renderValue(value) {
  if (Array.isArray(value)) {
    return value.join(", ");
  }

  if (value === null || value === undefined || value === "") {
    return "-";
  }

  if (typeof value === "object") {
    return JSON.stringify(value);
  }

  return String(value);
}

function DataCards({ title, items, fields, emptyMessage }) {
  return (
    <article className="action-card">
      <h3>{title}</h3>
      {!Array.isArray(items) || items.length === 0 ? (
        <p className="muted">{emptyMessage}</p>
      ) : (
        <div className="data-card-grid">
          {items.map((item, index) => (
            <div className="data-card" key={`${title}-${index}`}>
              {fields.map((field) => (
                <div key={field.key} className="data-row">
                  <span>{field.label}</span>
                  <strong>{renderValue(item[field.key])}</strong>
                </div>
              ))}
            </div>
          ))}
        </div>
      )}
    </article>
  );
}

export default DataCards;