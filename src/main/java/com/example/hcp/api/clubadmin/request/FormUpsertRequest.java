// src/main/java/com/example/hcp/api/clubadmin/request/FormUpsertRequest.java
package com.example.hcp.api.clubadmin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FormUpsertRequest(
        @NotEmpty
        @Size(max = 50)
        List<
                @NotBlank
                @Size(max = 100)
                        String
                > labels
) {}
