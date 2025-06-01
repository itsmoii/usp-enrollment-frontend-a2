package com.example.uspenrolme.models;

public class RegisteredCourseItem {
    private final String term;
    private final String courseCode;
    private final String title;
    private final String campus;
    private final String mode;
    private final String status;

    public RegisteredCourseItem(String term, String courseCode, String title, String campus, String mode, String status) {
        this.term = term;
        this.courseCode = courseCode;
        this.title = title;
        this.campus = campus;
        this.mode = mode;
        this.status = status;
    }

    public String getTerm() { return term; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public String getCampus() { return campus; }
    public String getMode() { return mode; }
    public String getStatus() { return status; }
} 