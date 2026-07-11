package com.example;

import java.util.ArrayList;
import java.util.List;

public class Subject {
    public List<String> existing() {
        return new ArrayList<>() {
            {
                add("value");
            }

            @Override
            public int size() {
                return super.size();
            }
        };
    }
}
