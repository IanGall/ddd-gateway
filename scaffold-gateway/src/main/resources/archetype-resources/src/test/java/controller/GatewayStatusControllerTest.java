package ${package}.controller;

import ${package}.model.GatewayStatus;
import cn.iantech.common.model.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayStatusControllerTest {

    @Test
    void 应返回当前网关状态() {
        Response<GatewayStatus> response = new GatewayStatusController("test-gateway").status();

        assertEquals("0000", response.getCode());
        assertEquals("test-gateway", response.getData().application());
        assertEquals("UP", response.getData().status());
    }
}
