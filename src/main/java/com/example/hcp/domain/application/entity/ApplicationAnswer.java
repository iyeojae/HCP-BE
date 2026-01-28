// src/main/java/com/example/hcp/domain/application/entity/ApplicationAnswer.java
package com.example.hcp.domain.application.entity;

import com.example.hcp.domain.form.entity.FormQuestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "application_answers")
public class ApplicationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private FormQuestion question;

    // ✅ 템플릿별 답변(JSON 또는 텍스트)을 그대로 저장
    @Setter
    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    public ApplicationAnswer() {}
}
