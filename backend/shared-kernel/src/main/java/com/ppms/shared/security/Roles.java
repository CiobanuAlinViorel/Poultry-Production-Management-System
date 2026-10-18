package com.ppms.shared.security;

/** Role codes seeded by admin-service (R__seed_roles_permissions.sql). */
public final class Roles {

    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    /** Read-only access to every module. */
    public static final String MANAGER = "MANAGER";
    public static final String HR_ADMIN = "HR_ADMIN";
    public static final String FLOCK_SUPERVISOR = "FLOCK_SUPERVISOR";
    public static final String FARM_MANAGER = "FARM_MANAGER";
    public static final String HATCHERY_SUPERVISOR = "HATCHERY_SUPERVISOR";
    public static final String SLAUGHTER_MANAGER = "SLAUGHTER_MANAGER";

    private Roles() {
    }
}
