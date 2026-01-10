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
    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Setter
    @Column(name = "type", nullable = false, length = 20)
    private String type; // TEXT/LONG_TEXT/SELECT/MULTISELECT 등

    @Setter
    @Column(name = "required", nullable = false)
    private boolean required;

    @Setter
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson; // 선택지 저장(필요 시)

    public FormQuestion() {}

}
