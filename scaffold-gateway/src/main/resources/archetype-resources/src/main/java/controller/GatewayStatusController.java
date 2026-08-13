#set( $symbol_dollar = '$' )
package ${package}.controller;

import ${package}.model.GatewayResponses;
import ${package}.model.GatewayStatus;
import cn.iantech.common.model.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class GatewayStatusController {

    private final String applicationName;

    public GatewayStatusController(@Value("${symbol_dollar}{spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    public Response<GatewayStatus> status() {
        return GatewayResponses.success(new GatewayStatus(applicationName, "UP"));
    }
}
