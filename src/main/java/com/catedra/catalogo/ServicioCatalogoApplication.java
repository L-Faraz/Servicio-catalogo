package com.catedra.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServicioCatalogoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicioCatalogoApplication.class, args);
    }

}
