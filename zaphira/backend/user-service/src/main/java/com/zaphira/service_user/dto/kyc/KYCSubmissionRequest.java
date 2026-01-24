package com.zaphira.service_user.dto.kyc;

import com.zaphira.service_user.model.enums.DocumentType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for submitting KYC documents
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCSubmissionRequest {
    
    @NotNull(message = "Document type is required")
    private DocumentType documentType;
    
    @NotBlank(message = "Document number is required")
    @Size(min = 5, max = 100, message = "Document number must be between 5 and 100 characters")
    private String documentNumber;
    
    @NotNull(message = "Document issue date is required")
    @PastOrPresent(message = "Document issue date cannot be in the future")
    private LocalDate documentIssueDate;
    
    @NotNull(message = "Document expiry date is required")
    @Future(message = "Document must not be expired")
    private LocalDate documentExpiryDate;
    
    @NotBlank(message = "Issuing country is required")
    @Size(max = 100, message = "Issuing country must not exceed 100 characters")
    private String documentIssuingCountry;
    
    @NotBlank(message = "Full name on document is required")
    @Size(max = 200, message = "Full name must not exceed 200 characters")
    private String fullNameOnDocument;
    
    @NotNull(message = "Date of birth on document is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirthOnDocument;
    
    @NotBlank(message = "Address on document is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String addressOnDocument;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
