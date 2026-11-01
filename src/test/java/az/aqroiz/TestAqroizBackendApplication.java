package az.aqroiz;

import org.springframework.boot.SpringApplication;

public class TestAqroizBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(AqroizBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
