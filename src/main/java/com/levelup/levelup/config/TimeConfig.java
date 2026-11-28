package com.levelup.levelup.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj de la aplicación: facilita pruebas deterministas con `Clock.fixed`. */
@Configuration
public class TimeConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
