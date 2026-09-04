package com.example.erp.contact.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactCreateRequest(
                @NotBlank String contactName,
                @NotBlank String contactType,
                @NotBlank String status) {
}
