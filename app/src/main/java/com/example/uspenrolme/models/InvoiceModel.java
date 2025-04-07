package com.example.uspenrolme.models;

import java.sql.Date;

public class InvoiceModel {
   private String courseName;
   private String courseCode;
   private String courseCampus;
   private String courseMode;
   private int semester;
   private int courseLevel;

   private String dueDate;
   private double price;

    public InvoiceModel(String courseName, String courseCode, String courseMode, String courseCampus,
                        int semester, int courseLevel, String dueDate, double price) {
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseMode = courseMode;
        this.courseCampus = courseCampus;
        this.semester = semester;
        this.courseLevel = courseLevel;
        this.dueDate = dueDate;
        this.price = price;
    }

    // Getters
    public String getCourseName() {
        return courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseCampus() {
        return courseCampus;
    }

    public String getCourseMode() {
        return courseMode;
    }

    public int getSemester() {
        return semester;
    }

    public int getCourseLevel() {
        return courseLevel;
    }

    public String getDueDate() {
        return dueDate;
    }

    public double getPrice() {
        return price;
    }

}
