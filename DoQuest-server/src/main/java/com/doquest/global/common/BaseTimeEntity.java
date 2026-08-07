package com.doquest.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 공통 매핑 정보가 필요한 상위 클래스에 선언하여 속성만 자식 엔티티에 제공
@EntityListeners(AuditingEntityListener.class) // JPA Entity Auditing 기능 활성화
public abstract class BaseTimeEntity {

    @CreatedDate // Entity 생성 시 시간 자동 저장
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // Entity 값 변경 시 시간 자동 수정
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}