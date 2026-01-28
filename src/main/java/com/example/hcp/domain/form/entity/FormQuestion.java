// src/main/java/com/example/hcp/domain/form/entity/FormQuestion.java
package com.example.hcp.domain.form.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "form_questions")
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

    // ✅ 질문 제목(길이 제한 없음)
    @Column(name = "label", nullable = false, columnDefinition = "TEXT")
    private String label;

    // ✅ 템플릿별 구성(단어/문장/하위질문 등) JSON
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    protected FormQuestion() {}

    // (호환용) 기존 생성자 유지
    public FormQuestion(ApplicationForm form, int orderNo, String label) {
        this.form = form;
        this.orderNo = orderNo;
        this.label = label;
        this.templateNo = 0;
        this.payloadJson = null;
    }

    // ✅ 신규 생성자
    public FormQuestion(ApplicationForm form, int orderNo, int templateNo, String label, String payloadJson) {
        this.form = form;
        this.orderNo = orderNo;
        this.templateNo = templateNo;
        this.label = label;
        this.payloadJson = payloadJson;
    }
}
