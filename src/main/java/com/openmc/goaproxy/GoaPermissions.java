package com.openmc.goaproxy;

public enum GoaPermissions {

    USE("goa.use");

    private final String permission;

    GoaPermissions(String s) {
        this.permission = s;
    }

    public String getPermission() {
        return permission;
    }
}
