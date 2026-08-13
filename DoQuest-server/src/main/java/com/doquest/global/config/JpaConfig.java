package com.doquest.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // JPA Auditing 기능을 독립 설정 클래스로 분리
public class JpaConfig {
}