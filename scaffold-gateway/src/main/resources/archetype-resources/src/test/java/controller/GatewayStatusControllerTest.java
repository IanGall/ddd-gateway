package ${package}.controller;

import ${package}.model.GatewayStatus;
import cn.iantech.common.model.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayStatusControllerTest {

    // 验证状态接口返回当前网关状态
    @Test
    void shouldReturnCurrentGatewayStatus() {
        Response<GatewayStatus> response = new GatewayStatusController("test-gateway").status();

        assertEquals("0000", response.getCode());
        assertEquals("test-gateway", response.getData().application());
        assertEquals("UP", response.getData().status());
    }
}
