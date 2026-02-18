package com.legalpro.accountservice.enums;

public enum CaseFinalStatus {

    NONE(0, "None"),
    WON(1, "Won"),
    LOST(2, "Lost");

    private final int code;
    private final String label;

    CaseFinalStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String getLabelByCode(Integer code) {
        if (code == null) return null;
        for (CaseFinalStatus s : values()) {
            if (s.code == code) return s.label;
        }
        return null;
    }
}
