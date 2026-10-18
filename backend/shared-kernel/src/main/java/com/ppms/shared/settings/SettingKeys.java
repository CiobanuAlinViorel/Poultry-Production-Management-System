package com.ppms.shared.settings;

public final class SettingKeys {

    private SettingKeys() {
    }

    // Identity
    public static final String IDENTITY_PASSWORD_MIN_LENGTH = "identity.password.minLength";
    public static final String IDENTITY_LOCKOUT_THRESHOLD = "identity.lockout.threshold";
    public static final String IDENTITY_LOCKOUT_AUTO_UNLOCK_MINUTES = "identity.lockout.autoUnlockMinutes";
    public static final String IDENTITY_MFA_REQUIRED_ROLES = "identity.mfa.requiredRoles";

    // Farm
    public static final String FA_STOCKING_MAX_KG_PER_M2 = "fa.stocking.maxKgPerM2";
    public static final String FA_DOWNTIME_MIN_DAYS = "fa.downtime.minDays";
}
