package com.ppms.shared.events;

public final class Topics {

    private Topics() {
    }

    public static final String IDENTITY_USER = "ppms.identity.user";
    public static final String ADMIN_ROLE_ASSIGNMENT = "ppms.admin.role-assignment";
    public static final String ADMIN_EMPLOYEE = "ppms.admin.employee";
    public static final String ADMIN_SETTINGS = "ppms.admin.settings";
    public static final String ADMIN_CATALOG = "ppms.admin.catalog";
    public static final String ADMIN_ROSTER = "ppms.admin.roster";
    public static final String SITE_DIRECTORY = "ppms.site.directory";
    public static final String PARENT_FLOCK_FLOCK = "ppms.parent-flock.flock";
    public static final String PARENT_FLOCK_PRODUCTION = "ppms.parent-flock.production";
    public static final String PARENT_FLOCK_EGG_TRANSFER = "ppms.parent-flock.egg-transfer";
    public static final String HATCHERY_EGG_RECEPTION = "ppms.hatchery.egg-reception";
    public static final String HATCHERY_INCUBATION = "ppms.hatchery.incubation";
    public static final String HATCHERY_CHICK_TRANSFER = "ppms.hatchery.chick-transfer";
    public static final String FARM_CHICK_RECEPTION = "ppms.farm.chick-reception";
    public static final String FARM_LOT = "ppms.farm.lot";
    public static final String FARM_DAILY_OPS = "ppms.farm.daily-ops";
    public static final String FARM_DELIVERY = "ppms.farm.delivery";
    public static final String SLAUGHTERHOUSE_RECEPTION = "ppms.slaughterhouse.reception";
    public static final String SLAUGHTERHOUSE_PROCESSING = "ppms.slaughterhouse.processing";
    public static final String SLAUGHTERHOUSE_COLD_CHAIN = "ppms.slaughterhouse.cold-chain";
    public static final String SLAUGHTERHOUSE_SALES = "ppms.slaughterhouse.sales";
    public static final String INVENTORY = "ppms.inventory";
    public static final String ALERTS = "ppms.alerts";
    public static final String NOTIFICATION = "ppms.notification";
    public static final String AUDIT = "ppms.audit";
}
