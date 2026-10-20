package com.example.scarletshopcore;

public enum Currency {
    MONEY("money"),
    SHARDS("shards");

    private final String key;

    Currency(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** Mengembalikan def jika input null, atau null jika input bukan mata uang yang valid. */
    public static Currency parse(String input, Currency def) {
        if (input == null) return def;
        for (Currency c : values()) {
            if (c.key.equalsIgnoreCase(input)) return c;
        }
        return null;
    }
}
