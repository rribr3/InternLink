package com.example.internlink;

public class ApplicationWithProject {
    private Application application;
    private Project project;

    public ApplicationWithProject(Application application, Project project) {
        this.application = application;
        this.project = project;
    }

    public Application getApplication() {
        return application;
    }

    public Project getProject() {
        return project;
    }
}