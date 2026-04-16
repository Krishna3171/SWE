function InlineFeedback({ feedback }) {
  if (!feedback?.message) {
    return null;
  }

  const className = feedback.type === "error" ? "feedback error" : "feedback success";
  return <p className={className}>{feedback.message}</p>;
}

export default InlineFeedback;