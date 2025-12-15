package com.zaphira.service_user.dto.request;

import com.zaphira.service_user.model.enums.DocumentType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class KYCSubmissionRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document number is required")
    @Size(max = 100)
    private String documentNumber;

    @NotNull(message = "Document issue date is required")
    @Past(message = "Issue date must be in the past")
    private LocalDate documentIssueDate;

    @NotNull(message = "Document expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate documentExpiryDate;

    @NotBlank(message = "Issuing country is required")
    @Size(max = 100)
    private String documentIssuingCountry;

    @NotBlank(message = "Full name on document is required")
    @Size(max = 200)
    private String fullNameOnDocument;

    @NotNull(message = "Date of birth on document is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirthOnDocument;

    @NotBlank(message = "Address on document is required")
    @Size(max = 500)
    private String addressOnDocument;

    @NotBlank(message = "Document front image is required")
    private String documentFrontImage;

    @SuppressWarnings("unused")
    private String documentBackImage;

    @NotBlank(message = "Selfie image is required")
    private String selfieImage;

    @SuppressWarnings("unused")
    private String proofOfAddressDocument;

    @Size(max = 1000)
    private String notes;
}
