package com.example;

public class Subject {
    public interface TextTransform<T> {
        T apply(T value);
    }
}
