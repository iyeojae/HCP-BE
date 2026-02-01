// src/main/java/com/example/hcp/domain/club/entity/ClubCategory.java
package com.example.hcp.domain.club.entity;

public enum ClubCategory {
    // 신규(표시용 7개)
    PERFORMANCE("공연분야"),
    SPORTS("체육분야"),
    ACADEMIC("학술분야"),
    VOLUNTEER("봉사분야"),
    ART("예술분야"),
    HOBBY("취미분야"),
    RELIGION("종교분야"),

    // 기존 데이터 호환(기존 값이 DB에 있어도 터지지 않게 유지)
    IT("학술분야"),
    CULTURE_ART("예술분야"),
    ETC("취미분야");

    private final String label;

    ClubCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    // 화면에 묶을 그룹(기존 값은 7개 중 하나로 매핑)
    public ClubCategory displayGroup() {
        return switch (this) {
            case IT -> ACADEMIC;
            case CULTURE_ART -> ART;
            case ETC -> HOBBY;
            default -> this;
        };
    }
}
