package com.thadeu.massa_dados_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.thadeu.massa_dados_core",
    "domain.entity"
})
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
