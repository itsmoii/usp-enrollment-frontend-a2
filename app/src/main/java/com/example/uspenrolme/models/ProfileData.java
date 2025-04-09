// ProfileData.java
package com.example.uspenrolme.models;

public class ProfileData {
    private String field;
    private String value;

    public ProfileData(String field, String value) {
        this.field = field;
        this.value = value;
    }

    public String getField() { return field; }
    public String getValue() { return value; }
}