package com.iteima.mysong;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@Slf4j
public class MysongApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysongApplication.class, args);
        log.info("  __ _  _         _       \n / _| || |       | |      \n" +
                "| |_| || |  __ _ | |  ___ \n|  _| || | / _` || | / _ \\\n| | " +
                "  || || (_| || ||  __/ \n|_|   |_(_)\\__,_||_|\\___|  \n");
    }


}
