package com.example.internlink;

public class Project {
    private final String title;
    private final String company;
    private final int companyLogo;
    private final String[] skills;
    private final String timeLeft;

    public Project(String title, String company, int companyLogo,
                   String[] skills, String timeLeft) {
        this.title = title;
        this.company = company;
        this.companyLogo = companyLogo;
        this.skills = skills;
        this.timeLeft = timeLeft;
    }

    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public int getCompanyLogo() { return companyLogo; }
    public String[] getSkills() { return skills; }
    public String getTimeLeft() { return timeLeft; }
}