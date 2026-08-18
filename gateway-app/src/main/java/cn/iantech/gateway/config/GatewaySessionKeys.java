package cn.iantech.gateway.config;

/**
 * 网关认证会话中的可信身份键。
 */
public final class GatewaySessionKeys {

    public static final String ACCOUNT_ID = "accountId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String USER_TYPE = "userType";
    public static final String PERMISSION_LIST = "permissionList";

    private GatewaySessionKeys() {
    }
}
