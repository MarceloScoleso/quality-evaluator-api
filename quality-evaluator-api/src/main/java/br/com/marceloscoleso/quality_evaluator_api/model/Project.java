package br.com.marceloscoleso.quality_evaluator_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Language language;

    private int linesOfCode;
    private int complexity;
    private boolean hasTests;
    private boolean usesGit;

   
    public Project() {
    }

    
    public Project(String name, Language language, int linesOfCode, int complexity,
                   boolean hasTests, boolean usesGit) {
        this.name = name;
        this.language = language;
        this.linesOfCode = linesOfCode;
        this.complexity = complexity;
        this.hasTests = hasTests;
        this.usesGit = usesGit;
    }


    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public boolean hasTests() {
        return hasTests;
    }

    public void setHasTests(boolean hasTests) {
        this.hasTests = hasTests;
    }

    public boolean usesGit() {
        return usesGit;
    }

    public void setUsesGit(boolean usesGit) {
        this.usesGit = usesGit;
    }
}