package ${package}.service;

import cn.iantech.api.IChannelAuthService;
import cn.iantech.api.model.channel.ChannelSignatureVerifyReq;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayChannelAuthClientTest {

    private IChannelAuthService service;
    private GatewayChannelAuthClient client;

    @BeforeEach
    void setUp() {
        service = mock(IChannelAuthService.class);
        client = new GatewayChannelAuthClient();
        ReflectionTestUtils.setField(client, "channelAuthService", service);
    }

    @Test
    void shouldPreserveAuthenticationFailure() {
        AppException expected = new AppException(Constants.ResponseCode.AUTH_REQUIRED.getCode(), "渠道认证失败");
        when(service.authenticate(any())).thenThrow(expected);

        AppException actual = assertThrows(AppException.class,
                () -> client.authenticate(new ChannelSignatureVerifyReq()));

        assertSame(expected, actual);
    }

    @Test
    void shouldMapRpcTimeout() {
        when(service.authenticate(any())).thenThrow(new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout"));

        AppException actual = assertThrows(AppException.class,
                () -> client.authenticate(new ChannelSignatureVerifyReq()));

        assertEquals(Constants.ResponseCode.RPC_TIMEOUT.getCode(), actual.getCode());
    }
}
