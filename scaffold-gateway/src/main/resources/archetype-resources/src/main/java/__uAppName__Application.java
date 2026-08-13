package ${package};

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 网关应用启动入口。 */
@SpringBootApplication
@EnableDubbo
public class ${uAppName}Application {

    public static void main(String[] args) {
        SpringApplication.run(${uAppName}Application.class, args);
    }
}
