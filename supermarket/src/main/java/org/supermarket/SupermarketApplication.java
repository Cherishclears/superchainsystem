package org.supermarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.supermarket.modules.*.mapper")
public class SupermarketApplication {

    public static void main(String[] args) {

        SpringApplication.run(SupermarketApplication.class, args);
    }

}
