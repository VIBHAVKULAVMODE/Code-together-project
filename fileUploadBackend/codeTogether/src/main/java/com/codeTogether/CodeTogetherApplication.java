package com.codeTogether;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication
@RestController
@RequestMapping("/api/files")
public class CodeTogetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeTogetherApplication.class, args);
    }
}
