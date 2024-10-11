package com.neshan.neshantask.data.model.response;

public class Distance {
    private int value; // مقدار فاصله به متر
    private String text; // توضیح فاصله

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
