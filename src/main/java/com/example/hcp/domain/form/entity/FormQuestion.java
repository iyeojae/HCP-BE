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

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    protected FormQuestion() {}

    public FormQuestion(ApplicationForm form, int orderNo, String label) {
        this.form = form;
        this.orderNo = orderNo;
        this.label = label;
    }
}
