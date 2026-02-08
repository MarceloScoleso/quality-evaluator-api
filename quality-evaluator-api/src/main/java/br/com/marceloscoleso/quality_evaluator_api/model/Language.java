package br.com.marceloscoleso.quality_evaluator_api.model;

public enum Language {

    JAVA("Java"),
    CSHARP("C#"),
    JAVASCRIPT("JavaScript"),
    TYPESCRIPT("TypeScript"),
    PYTHON("Python"),
    KOTLIN("Kotlin"),
    GO("Go"),
    PHP("PHP"),
    RUBY("Ruby"),
    SWIFT("Swift"),
    C("C"),
    CPP("C++"),
    RUST("Rust"),
    DART("Dart"),
    OTHER("Outra");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Language fromOption(int option) {
        return values()[option - 1];
    }
}
