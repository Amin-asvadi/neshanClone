package com.neshan.neshantask.data.model.response;

public class Duration {
    private int value; // مقدار زمان به ثانیه
    private String text; // توضیح زمان

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
