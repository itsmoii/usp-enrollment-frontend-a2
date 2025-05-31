package com.example.uspenrolme.models;

public class HoldRulesModel {
    private boolean registration;
    private boolean grades;
    private boolean audit;
    private boolean recheck;
    private boolean graduation;

    // Constructor
    public HoldRulesModel(boolean registration, boolean grades, boolean audit, boolean recheck, boolean graduation) {
        this.registration = registration;
        this.grades = grades;
        this.audit = audit;
        this.recheck = recheck;
        this.graduation = graduation;
    }

    // Getters
    public boolean isRegistration() {
        return registration;
    }

    public boolean isGrades() {
        return grades;
    }

    public boolean isAudit() {
        return audit;
    }

    public boolean isRecheck() {
        return recheck;
    }

    public boolean isGraduation() {
        return graduation;
    }

    // Setters

    public void setGrades(boolean grades) {
        this.grades = grades;
    }

    public void setAudit(boolean audit) {
        this.audit = audit;
    }

    public void setRegistration(boolean registration) {
        this.registration = registration;
    }

    public void setRecheck(boolean recheck) {
        this.recheck = recheck;
    }

    public void setGraduation(boolean graduation) {
        this.graduation = graduation;
    }
}
