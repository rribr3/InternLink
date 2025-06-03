package com.example.internlink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class User {
    // Common fields for all users
    private String name;
    private String email;
    private String role;
    private String industry;
    private String logoUrl;
    private String userId;
    private String location;
    private String description;
    private String phone;

    // Student-specific fields
    private String university;
    private String fieldOfStudy;
    private String graduationYear;
    private String bio;
    private Object skills; // Changed to Object to handle both String and List
    private String profilePictureUrl;
    private String gpa;
    private String degree;
    private String major;
    private String minor;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String resume;
    private String dateOfBirth;
    private String gender;
    private String nationality;
    private String languages;
    private String experience;
    private String certifications;
    private String projects;
    private String achievements;
    private String interests;
    private String availability;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Getters and setters for common fields
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getUid() {
        return userId;
    }

    public void setUid(String userId) {
        this.userId = userId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // Getters and setters for student-specific fields
    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    public String getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(String graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    // Special handling for skills field
    public List<String> getSkills() {
        if (skills == null) {
            return new ArrayList<>();
        }

        if (skills instanceof List) {
            // If it's already a List, cast and return
            try {
                @SuppressWarnings("unchecked")
                List<String> skillsList = (List<String>) skills;
                return skillsList != null ? skillsList : new ArrayList<>();
            } catch (ClassCastException e) {
                // If casting fails, convert to string and parse
                return parseSkillsFromString(skills.toString());
            }
        } else if (skills instanceof String) {
            // If it's a String, parse it
            return parseSkillsFromString((String) skills);
        } else {
            // For any other type, convert to string and parse
            return parseSkillsFromString(skills.toString());
        }
    }

    // Single setter method that accepts Object to handle both String and List
    public void setSkills(Object skills) {
        this.skills = skills;
    }

    // Helper method to parse skills from string
    private List<String> parseSkillsFromString(String skillsString) {
        if (skillsString == null || skillsString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Try different parsing methods
        List<String> skillsList = new ArrayList<>();

        // Method 1: Split by comma
        if (skillsString.contains(",")) {
            String[] skillsArray = skillsString.split(",");
            for (String skill : skillsArray) {
                String trimmedSkill = skill.trim();
                if (!trimmedSkill.isEmpty()) {
                    skillsList.add(trimmedSkill);
                }
            }
        }
        // Method 2: Split by semicolon
        else if (skillsString.contains(";")) {
            String[] skillsArray = skillsString.split(";");
            for (String skill : skillsArray) {
                String trimmedSkill = skill.trim();
                if (!trimmedSkill.isEmpty()) {
                    skillsList.add(trimmedSkill);
                }
            }
        }
        // Method 3: Split by pipe
        else if (skillsString.contains("|")) {
            String[] skillsArray = skillsString.split("\\|");
            for (String skill : skillsArray) {
                String trimmedSkill = skill.trim();
                if (!trimmedSkill.isEmpty()) {
                    skillsList.add(trimmedSkill);
                }
            }
        }
        // Method 4: Single skill
        else {
            skillsList.add(skillsString.trim());
        }

        return skillsList;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getGpa() {
        return gpa;
    }

    public void setGpa(String gpa) {
        this.gpa = gpa;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getCertifications() {
        return certifications;
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public String getProjects() {
        return projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getAchievements() {
        return achievements;
    }

    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    // Utility methods for search functionality
    public String getSearchableText() {
        StringBuilder searchText = new StringBuilder();

        // Add all text fields to searchable content
        appendIfNotNull(searchText, name);
        appendIfNotNull(searchText, email);
        appendIfNotNull(searchText, university);
        appendIfNotNull(searchText, fieldOfStudy);
        appendIfNotNull(searchText, graduationYear);
        appendIfNotNull(searchText, bio);
        appendIfNotNull(searchText, location);
        appendIfNotNull(searchText, phone);
        appendIfNotNull(searchText, major);
        appendIfNotNull(searchText, minor);
        appendIfNotNull(searchText, degree);
        appendIfNotNull(searchText, gpa);
        appendIfNotNull(searchText, experience);
        appendIfNotNull(searchText, certifications);
        appendIfNotNull(searchText, projects);
        appendIfNotNull(searchText, achievements);
        appendIfNotNull(searchText, interests);
        appendIfNotNull(searchText, languages);
        appendIfNotNull(searchText, nationality);
        appendIfNotNull(searchText, availability);

        // Add skills if available
        List<String> skillsList = getSkills();
        if (skillsList != null) {
            for (String skill : skillsList) {
                appendIfNotNull(searchText, skill);
            }
        }

        return searchText.toString().toLowerCase();
    }

    private void appendIfNotNull(StringBuilder sb, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(value.trim());
        }
    }

    // Convenience method to check if user is a student
    public boolean isStudent() {
        return "student".equalsIgnoreCase(role);
    }

    // Convenience method to check if user is a company
    public boolean isCompany() {
        return "company".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", university='" + university + '\'' +
                ", fieldOfStudy='" + fieldOfStudy + '\'' +
                ", graduationYear='" + graduationYear + '\'' +
                '}';
    }
}