// Registration.java
package com.example.uspenrolme.models;

public class Registration {
    private String courseCode;
    private String courseName;
    private String courseCampus;
    private String courseMode;
    private String status;

    public Registration(String courseCode, String courseName, String courseCampus, String courseMode, String status) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.courseCampus = courseCampus;
        this.courseMode = courseMode;
        this.status = status;
    }

    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getCourseCampus() { return courseCampus; }
    public String getCourseMode() { return courseMode; }
    public String getStatus() { return status; }
}
