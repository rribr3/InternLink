package com.example.internlink;

import java.util.List;
import java.util.Map;

public class Project {
    private String title;
    private String description;
    private List<String> skills;
    private String category;
    private String duration;
    private String startDate;
    private String deadline;
    private int studentsRequired;
    private String educationLevel;
    private String compensationType;
    private String amount;
    private String companyId;
    private String companyName;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private Quiz quiz;
    private String status;

    // Empty constructor for Firebase
    public Project() {
    }

    // Getters and setters for all fields
    public String getTitle() {
        return title;
    }
    public String getStatus() {
        return status;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
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