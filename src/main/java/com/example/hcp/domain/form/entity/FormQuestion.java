// src/main/java/com/example/hcp/domain/form/entity/FormQuestion.java
package com.example.hcp.domain.form.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "form_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_form_questions_form_order", columnNames = {"form_id", "order_no"})
        }
)
public class FormQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private ApplicationForm form;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    // ✅ 1~6 템플릿 번호
    @Column(name = "template_no", nullable = false)
    private int templateNo;

    // ✅ 블록 제목(길이 제한 없음)
    @Column(name = "label", nullable = false, columnDefinition = "TEXT")
    private String label;

    // ✅ 템플릿별 구성 JSON
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    protected FormQuestion() {}

    public FormQuestion(ApplicationForm form, int orderNo, int templateNo, String label, String payloadJson) {
        this.form = form;
        this.orderNo = orderNo;
        this.templateNo = templateNo;
        this.label = label;
        this.payloadJson = payloadJson;
    }
}
