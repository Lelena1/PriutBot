package com.example.priutbot.entity;

public enum ShelterType {
    CAT("Приют для кошек"),
    DOG("Приют для собак");

    private final String displayName;

    ShelterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
