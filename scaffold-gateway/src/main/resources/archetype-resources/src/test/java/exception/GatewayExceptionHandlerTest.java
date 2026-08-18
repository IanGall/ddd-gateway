package ${package}.exception;

import cn.iantech.common.model.Response;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayExceptionHandlerTest {

    private final GatewayExceptionHandler handler = new GatewayExceptionHandler();

    @Test
    void shouldMapDubboFailureToBadGateway() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.NETWORK_EXCEPTION, "下游连接失败"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("RPC_ERROR", response.getBody().getCode());
        assertEquals("下游服务调用失败", response.getBody().getInfo());
    }
}
