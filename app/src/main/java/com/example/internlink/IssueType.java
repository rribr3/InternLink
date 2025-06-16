package com.example.internlink;

public enum IssueType {
    BUG("Bug", "🐛"),
    FEATURE_REQUEST("Feature Request", "✨"),
    QUESTION("Question", "❓"),
    ENHANCEMENT("Enhancement", "📈"),
    DOCUMENTATION("Documentation", "📚"),
    SECURITY("Security", "🔒");

    private final String label;
    private final String emoji;

    IssueType(String label, String emoji) {
        this.label = label;
        this.emoji = emoji;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    @Override
    public String toString() {
        return emoji + " " + label;
    }
}