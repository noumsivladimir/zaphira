package com.zaphira.service_user.mapper;

import com.zaphira.service_user.dto.response.KYCStatusResponse;
import com.zaphira.service_user.model.entities.KYC;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KYCMapper {

    public KYCStatusResponse toResponse(KYC kyc) {
        if (kyc == null) {
            return null;
        }

        return KYCStatusResponse.builder()
                .kycId(kyc.getKycId())
                .userId(kyc.getUser().getUserId())
                .kycStatus(kyc.getKycStatus())
                .documentType(kyc.getDocumentType())
                .documentNumber(kyc.getDocumentNumber())
                .documentIssueDate(kyc.getDocumentIssueDate())
                .documentExpiryDate(kyc.getDocumentExpiryDate())
                .documentIssuingCountry(kyc.getDocumentIssuingCountry())
                .fullNameOnDocument(kyc.getFullNameOnDocument())
                .dateOfBirthOnDocument(kyc.getDateOfBirthOnDocument())
                .addressOnDocument(kyc.getAddressOnDocument())
                .documentFrontImage(kyc.getDocumentFrontImage())
                .documentBackImage(kyc.getDocumentBackImage())
                .selfieImage(kyc.getSelfieImage())
               // .proofOfAddressDocument(kyc.getProofOfAddressDocument())
                .submittedAt(kyc.getSubmittedAt())
                .verifiedAt(kyc.getVerifiedAt())
           //     .verifiedBy(kyc.getVerifiedBy())
                .rejectionReason(kyc.getRejectionReason())
                .rejectedAt(kyc.getRejectedAt())
      //          .rejectedBy(kyc.getRejectedBy())
                .resubmissionCount(kyc.getResubmissionCount())
       //         .canResubmit(kyc.canResubmit())
       //         .isExpired(kyc.isExpired())
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())
                .build();
    }

    public List<KYCStatusResponse> toResponseList(List<KYC> kycList) {
        if (kycList == null || kycList.isEmpty()) {
            return List.of();
        }

        return kycList.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}