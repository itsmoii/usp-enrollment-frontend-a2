package com.example.uspenrolme.models;

public class PaymentModel {

    private String date;
    private double amount;
    private String method;

    public PaymentModel(String date, double amount, String method) {
        this.date = date;
        this.amount = amount;
        this.method = method;
    }

    public String getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public String getMethod() {
        return method;
    }
}
