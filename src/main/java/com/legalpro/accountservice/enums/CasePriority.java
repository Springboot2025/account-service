package com.legalpro.accountservice.enums;

public enum CasePriority {

    NORMAL(0, "Normal"),
    HIGH(1, "High Priority"),
    URGENT(2, "Urgent");

    private final int code;
    private final String label;

    CasePriority(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String getLabelByCode(Integer code) {
        if (code == null) return null;
        for (CasePriority p : values()) {
            if (p.code == code) return p.label;
        }
        return null;
    }
}
