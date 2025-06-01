package com.example.uspenrolme.models;

import java.io.Serializable;

public class ApplicationsModel implements Serializable {
    private int id;
    private String date;
    private String type;
    private String status;

    public ApplicationsModel(int id, String date, String type, String status) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }
}
