package com.example;

record Point(int x, int y) {
}

record Segment(Point start, Point end) {
}

public class Subject {
    public int coordinateSum(Object value) {
        if (value instanceof Segment(Point(int x1, int y1), Point(int x2, int y2))) {
            return x1 + y1 + x2 + y2;
        }
        return 0;
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
