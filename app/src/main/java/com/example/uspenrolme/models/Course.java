package com.example.uspenrolme.models;

public class Course {
    private String courseCode;
    private String title;
    private String campus;
    private String mode;
    private String semester;
    private String preRequisite; // New field for prerequisites
    private String grade; // New field for grade
    private boolean isSelected;

    // Constructor, getters, and setters
    public Course(String courseCode, String title, String campus, String mode, String semester, String preRequisite) {
        this.courseCode = courseCode;
        this.title = title;
        this.campus = campus;
        this.mode = mode;
        this.semester = semester;
        this.preRequisite = preRequisite;
        this.grade = null; // Initialize grade as null
        this.isSelected = false;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getPreRequisite() {
        return preRequisite;
    }

    public void setPreRequisite(String preRequisite) {
        this.preRequisite = preRequisite;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}