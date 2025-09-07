package com.example.medicineApp.database;

import java.util.HashMap;
import java.util.Map;

public enum TimeTermStatus {
    BEFORE_BREAKFAST(1, "Before breakfast"),
    AT_BREAKFAST(2, "At breakfast"),
    AFTER_BREAKFAST(3, "After breakfast"),
    BEFORE_LUNCH(4, "Before lunch"),
    AT_LUNCH(5, "At lunch"),
    AFTER_LUNCH(6, "After lunch"),
    BEFORE_DINNER(7, "Before dinner"),
    AT_DINNER(8, "At dinner"),
    AFTER_DINNER(9, "After dinner");

    private final int id;
    private final String label;

    private static final Map<Integer, TimeTermStatus> ID_MAP = new HashMap<>();

    static {
        for (TimeTermStatus status : values()) {
            ID_MAP.put(status.id, status);
        }
    }

    TimeTermStatus(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public static String labelForId(int id) {
        TimeTermStatus status = ID_MAP.get(id);
        return status != null ? status.label : "Unknown";
    }
}
