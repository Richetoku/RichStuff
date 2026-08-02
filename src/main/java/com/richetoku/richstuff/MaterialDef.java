package com.richetoku.richstuff;

public record MaterialDef(String name, String kind, String color, int tier, String parent1, String parent2) {
    public boolean isGemLike() { return kind.equals("gem") || kind.equals("crystal"); }
    public boolean isMetalLike() { return kind.equals("metal") || kind.equals("alloy"); }
}
