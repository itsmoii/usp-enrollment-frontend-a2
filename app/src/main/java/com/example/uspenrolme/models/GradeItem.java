package com.example.uspenrolme.models;

public class GradeItem {
    private final String term;
    private final String courseCode;
    private final String title;
    private final String campus;
    private final String mode;
    private final String grade;

    public GradeItem(String term, String courseCode, String title, String campus, String mode, String grade) {
        this.term = term;
        this.courseCode = courseCode;
        this.title = title;
        this.campus = campus;
        this.mode = mode;
        this.grade = grade;
    }

    public String getTerm() { return term; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public String getCampus() { return campus; }
    public String getMode() { return mode; }
    public String getGrade() { return grade; }
} 