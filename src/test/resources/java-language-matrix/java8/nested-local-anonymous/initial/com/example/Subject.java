package com.example;

public class Subject {
    public Object helper() {
        class LocalHelper {
            public String addedBehavior() {
                return "local";
            }
        }
        return new Object() {
            public String addedBehavior() {
                return new LocalHelper().addedBehavior();
            }
        };
    }
}

class SecondaryType {
    public String addedBehavior() {
        return "secondary";
    }
}
