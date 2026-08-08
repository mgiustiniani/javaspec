package com.example;

public final class Calculator {
    private final int bias;

    public Calculator(int bias) {
        this.bias = bias;
    }

    public int add(int left, int right) {
        return bias + left + right;
    }
}
