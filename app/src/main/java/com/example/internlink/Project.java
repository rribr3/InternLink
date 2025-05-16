package com.example.internlink;

import java.util.List;
import java.util.Map;

public class Project {
    private String title;
    private String description;
    private List<String> skills;
    private String category;
    private String duration;
    private long startDate;
    private long deadline;
    private int studentsRequired;
    private int applicants;
    private String educationLevel;
    private String compensationType;
    private int amount;
    private String companyId;
    private String companyName;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private Quiz quiz;
    private String status;
    private String projectId;

    // Empty constructor for Firebase
    public Project() {
    }

    // Getters and setters for all fields
    public String getTitle() {
        return title;
    }
    public int getApplicants() {
        return applicants;
    }
    public void setApplicants(int applicants) {
        this.applicants = applicants;
    }
    public String getStatus() {
        return status;
    }
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public int getStudentsRequired() {
        return studentsRequired;
    }

    public void setStudentsRequired(int studentsRequired) {
        this.studentsRequired = studentsRequired;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getCompensationType() {
        return compensationType;
    }

    public void setCompensationType(String compensationType) {
        this.compensationType = compensationType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public int getCompanyLogo() {
        return 0;
    }

    public int getTimeLeft() {
        return 0;
    }
}