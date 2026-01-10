// src/main/java/com/example/hcp/domain/form/entity/FormQuestion.java
package com.example.hcp.domain.form.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "form_questions")
public class FormQuestion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "form_id", nullable = false)
    private ApplicationForm form;

    @Setter
    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Setter
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    // DB 컬럼 유지(폼 저장 시 고정값 세팅)
    @Setter
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Setter
    @Column(name = "required", nullable = false)
    private boolean required;

    @Setter
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    public FormQuestion() {}
}
