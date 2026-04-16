function FormField({ label, children, compact = false, error }) {
  return (
    <label className={compact ? "field field-compact" : "field"}>
      <span>{label}</span>
      {children}
      {error ? <small className="field-error">{error}</small> : null}
    </label>
  );
}

export default FormField;