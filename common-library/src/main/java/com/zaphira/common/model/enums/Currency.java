package com.zaphira.common.model.enums;

public enum Currency {
    XAF("Franc CFA", "FCFA", 0);

    private final String name;
    private final String symbol;
    private final int decimalPlaces;

    Currency(String name, String symbol, int decimalPlaces) {
        this.name = name;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}