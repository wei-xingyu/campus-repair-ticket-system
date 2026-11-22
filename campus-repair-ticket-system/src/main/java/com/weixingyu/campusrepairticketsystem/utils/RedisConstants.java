package com.weixingyu.campusrepairticketsystem.utils;

public class RedisConstants {
    public static final String LOGIN_USER_KEY="login:token:";
    public static final Long LOGIN_USER_TTL=30L;
    public static final String TICKET_DETAILED_KEY="cache:ticket:detailed:";
    public static final long TICKET_DETAILED_TTL = 15L;
    public static final Long NULL_OBJECT_TTL=1L;
    public static final String CATEGORIES_KEY="cache:categories:";
    public static final Long CATEGORIES_TTL=30L;
    public static final String BUILDING_KEY = "cache:building:";
    public static final Long BUILDING_TTL = 30L;
    public static final String BUILDING_ASSET_KEY = "cache:building:asset:";
    public static final Long BUILDING_ASSET_TTL = 10L;
    public static final String USER_TOKENS_PREFIX = "login:user:";


}
