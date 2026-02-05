package com.zaphira.user.model.enums;



public enum RoleType  {


    REGULAR_USER("Regular User", "Standard user with basic wallet operations"),
    ADMIN("Administrator", "System administrator with elevated privileges"),
    MERCHANT("Merchant", "Business user who can accept payments"),
    AGENT("Agent", "Business user who can deposit and withdraw funds"),
    SUPPORT("Support Agent", "Customer support representative"),
    SUPER_ADMIN("Super Administrator", "Full system access and control");

    private final String displayName;
    private final String description;

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    RoleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }


}
