package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * EvidenceRequest DTO - Request object for submitting evidence in a dispute.
 * 
 * Both customers and merchants can submit evidence to support their position.
 * This is a multipart request that combines file upload with metadata.
 * 
 * Validation Rules:
 * - file: Required, must be a valid file (max 50MB)
 * - evidenceType: Required, must be one of predefined types
 * - description: Optional, can provide context about the evidence
 * 
 * Example (as multipart form data):
 * - file: <binary file content>
 * - evidenceType: RECEIPT
 * - description: "Receipt for order #12345 showing full transaction details"
 * 
 * Supported Evidence Types:
 * RECEIPT, INVOICE, DELIVERY_PROOF, COMMUNICATION, REFUND_PROOF,
 * PROOF_OF_IDENTITY, PRODUCT_PHOTO, CONTRACT, EMAIL, SCREENSHOT, OTHER
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to submit evidence in a dispute (multipart form data)")
public class EvidenceRequest {
    
    /**
     * The file to be uploaded as evidence
     * Maximum size: 50 MB
     * Accepted formats: PDF, PNG, JPG, DOC, DOCX, TXT, etc
     */
    @NotNull(message = "File is required")
    @Schema(
        description = "File to upload as evidence (max 50MB)",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MultipartFile file;
    
    /**
     * Type of evidence being submitted
     * Possible values: RECEIPT, INVOICE, DELIVERY_PROOF, COMMUNICATION,
     * REFUND_PROOF, PROOF_OF_IDENTITY, PRODUCT_PHOTO, CONTRACT, EMAIL, SCREENSHOT, OTHER
     */
    @NotBlank(message = "Evidence type is required")
    @JsonProperty("evidence_type")
    @Schema(
        description = "Type of evidence being submitted",
        example = "RECEIPT",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"RECEIPT", "INVOICE", "DELIVERY_PROOF", "COMMUNICATION", 
                          "REFUND_PROOF", "PROOF_OF_IDENTITY", "PRODUCT_PHOTO"}
    )
    private String evidenceType;
    
    /**
     * Optional description of the evidence
     * Helps reviewers understand context of the submitted file
     */
    @JsonProperty("description")
    @Schema(
        description = "Description of the evidence (optional)",
        example = "Receipt showing transaction was completed and funds charged",
        maxLength = 1000,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String description;
}
