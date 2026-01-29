// src/main/java/com/example/hcp/api/student/FormResponse.java
package com.example.hcp.api.student;

import java.util.List;

public record FormResponse(
        Integer totalItems,
        List<Item> items
) {
    public record Item(
            Integer orderNo,
            Integer templateNo,
            String title,
            Object payload
    ) {}
}
