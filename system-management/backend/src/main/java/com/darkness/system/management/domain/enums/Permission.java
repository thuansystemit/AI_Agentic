package com.darkness.system.management.domain.enums;

public enum Permission {
    READ(1), WRITE(2), EDIT(3);

    private final int level;

    Permission(int level) { this.level = level; }

    public int getLevel() { return level; }

    public boolean isAtLeast(Permission required) {
        return this.level >= required.level;
    }
}
