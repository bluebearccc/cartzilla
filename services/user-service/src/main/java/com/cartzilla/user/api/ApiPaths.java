package com.cartzilla.user.api;

public final class ApiPaths {
    private ApiPaths() {}

    public static final String USERS = "/api/users";
    public static final String USER_ME = USERS + "/me";
    public static final String USER_ADDRESSES = USER_ME + "/addresses";
    public static final String VOUCHERS = "/api/vouchers";
    public static final String ADMIN_USERS = "/api/admin/users";
    public static final String ADMIN_VOUCHERS = "/api/admin/vouchers";
    public static final String INTERNAL_USERS = "/api/internal/users";
    public static final String INTERNAL_VOUCHERS = "/api/internal/vouchers";
}
