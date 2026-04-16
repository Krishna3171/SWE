function SectionTabs({ sections, activeSection, onSelect }) {
  return (
    <section className="nav-strip" aria-label="Dashboard sections">
      {sections.map((section) => (
        <button
          key={section.id}
          type="button"
          className={activeSection === section.id ? "nav-pill active" : "nav-pill"}
          onClick={() => onSelect(section.id)}
        >
          {section.label}
        </button>
      ))}
    </section>
  );
}

export default SectionTabs;