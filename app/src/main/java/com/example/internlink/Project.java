package com.example.internlink;

import android.util.Log;
import java.util.List;

public class Project {
    private String title;
    private String description;
    private List<String> skills;
    private String category;
    private String duration;
    private Object startDate; // ✅ Changed to Object to handle both long and string
    private Object deadline; // ✅ Changed to Object to handle both long and string
    private Object studentsRequired; // ✅ Changed to Object to handle both int and string
    private Object applicants; // ✅ Changed to Object to handle both int and string
    private String educationLevel;
    private String compensationType;
    private Object amount; // ✅ Changed to Object to handle both int and string
    private String companyId;
    private String companyName;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private Quiz quiz;
    private String status;
    private String projectId;
    private Object createdAt; // ✅ Changed to Object to handle both long and string
    private boolean resumeRequired;
    private String companyLogoUrl;
    private String location;

    // Empty constructor for Firebase
    public Project() {
    }

    // Getters and setters for all fields
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    // ✅ SAFE GETTER: Handles both long and string types
    public long getStartDate() {
        if (startDate instanceof Long) {
            return (Long) startDate;
        } else if (startDate instanceof String) {
            try {
                return Long.parseLong((String) startDate);
            } catch (NumberFormatException e) {
                Log.w("Project", "Invalid startDate format: " + startDate);
                return System.currentTimeMillis(); // default value
            }
        } else if (startDate instanceof Number) {
            return ((Number) startDate).longValue();
        }
        return System.currentTimeMillis(); // default if null
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setStartDate(Object startDate) {
        this.startDate = startDate;
    }

    // ✅ SAFE GETTER: Handles both long and string types
    public long getDeadline() {
        if (deadline instanceof Long) {
            return (Long) deadline;
        } else if (deadline instanceof String) {
            try {
                return Long.parseLong((String) deadline);
            } catch (NumberFormatException e) {
                Log.w("Project", "Invalid deadline format: " + deadline);
                return System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L); // default: 7 days from now
            }
        } else if (deadline instanceof Number) {
            return ((Number) deadline).longValue();
        }
        return System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L); // default if null
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setDeadline(Object deadline) {
        this.deadline = deadline;
    }

    // ✅ SAFE GETTER: Handles both int and string types
    public int getStudentsRequired() {
        if (studentsRequired instanceof Integer) {
            return (Integer) studentsRequired;
        } else if (studentsRequired instanceof String) {
            try {
                return Integer.parseInt((String) studentsRequired);
            } catch (NumberFormatException e) {
                Log.w("Project", "Invalid studentsRequired format: " + studentsRequired);
                return 1; // default value
            }
        } else if (studentsRequired instanceof Long) {
            return ((Long) studentsRequired).intValue();
        } else if (studentsRequired instanceof Number) {
            return ((Number) studentsRequired).intValue();
        }
        return 1; // default if null
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setStudentsRequired(Object studentsRequired) {
        this.studentsRequired = studentsRequired;
    }

    // ✅ SAFE GETTER: Handles both int and string types
    public int getApplicants() {
        if (applicants instanceof Integer) {
            return (Integer) applicants;
        } else if (applicants instanceof String) {
            try {
                return Integer.parseInt((String) applicants);
            } catch (NumberFormatException e) {
                Log.w("Project", "Invalid applicants format: " + applicants);
                return 0; // default value
            }
        } else if (applicants instanceof Long) {
            return ((Long) applicants).intValue();
        } else if (applicants instanceof Number) {
            return ((Number) applicants).intValue();
        }
        return 0; // default if null
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setApplicants(Object applicants) {
        this.applicants = applicants;
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

    // ✅ SAFE GETTER: Handles both int and string types
    public int getAmount() {
        if (amount instanceof Integer) {
            return (Integer) amount;
        } else if (amount instanceof String) {
            try {
                return Integer.parseInt((String) amount);
            } catch (NumberFormatException e) {
                Log.w("Project", "Invalid amount format: " + amount);
                return 0; // default value
            }
        } else if (amount instanceof Long) {
            return ((Long) amount).intValue();
        } else if (amount instanceof Number) {
            return ((Number) amount).intValue();
        }
        return 0; // default if null
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setAmount(Object amount) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    // ✅ SAFE GETTER: Handles both long and string types
    public long getCreatedAt() {
        if (createdAt instanceof Long) {
            return (Long) createdAt;
        } else if (createdAt instanceof String) {
            try {
                return Long.parseLong((String) createdAt);
            } catch (NumberFormatException e) {
                Log.w("Project", "Invalid createdAt format: " + createdAt);
                return System.currentTimeMillis(); // default value
            }
        } else if (createdAt instanceof Number) {
            return ((Number) createdAt).longValue();
        }
        return System.currentTimeMillis(); // default if null
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isResumeRequired() {
        return resumeRequired;
    }

    public void setResumeRequired(boolean resumeRequired) {
        this.resumeRequired = resumeRequired;
    }

    public String getCompanyLogoUrl() {
        return companyLogoUrl;
    }

    public void setCompanyLogoUrl(String companyLogoUrl) {
        this.companyLogoUrl = companyLogoUrl;
    }

    // Helper method to check if project is active
    public boolean isActive() {
        return "approved".equalsIgnoreCase(status) &&
                getDeadline() > System.currentTimeMillis(); // Use safe getter
    }

    // Helper method to check if project has quiz
    public boolean hasQuiz() {
        return quiz != null;
    }

    // Helper method to get time left until deadline in days
    public int getTimeLeftInDays() {
        long deadlineValue = getDeadline(); // Use safe getter
        if (deadlineValue <= 0) return 0;
        long currentTime = System.currentTimeMillis();
        if (deadlineValue <= currentTime) return 0;
        return (int) ((deadlineValue - currentTime) / (1000 * 60 * 60 * 24));
    }

    public boolean hasEnded() {
        long deadlineValue = getDeadline(); // Use safe getter
        return deadlineValue > 0 && deadlineValue < System.currentTimeMillis();
    }
}