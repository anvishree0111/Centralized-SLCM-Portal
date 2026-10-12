package com.slcm.portal.finance;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class FinanceDtos {
    private FinanceDtos() {}

    public record DocumentTypeInput(@NotBlank @Size(max=40) String code,
            @NotBlank @Size(max=100) String name, @NotNull @Size(max=255) String description,
            @NotNull Boolean active) {}
    public record DocumentInput(@NotNull @Positive Long documentTypeId, @Size(max=255) String purpose) {}
    public enum DocumentStatus { PROCESSING, APPROVED, REJECTED }
    public record DocumentReview(@NotNull DocumentStatus status, @Size(max=4000) String remarks) {}
    public record FeeInput(@NotNull @Positive Long studentId, @NotNull @Positive Long semesterId,
            @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal totalAmount,
            @NotNull LocalDate dueDate) {}
    public enum PaymentMode { UPI, CARD, NET_BANKING, CASH, BANK_TRANSFER }
    public record PaymentInput(@NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal amount,
            @NotNull PaymentMode mode, @NotBlank @Size(max=100) String transactionReference) {}
    public record AdminPaymentInput(@NotNull @Positive Long feeRecordId,
            @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal amount,
            @NotNull PaymentMode mode, @NotBlank @Size(max=100) String transactionReference) {}
    public record DocumentTypeView(long id, String code, String name, String description, boolean active) {}
    public record DocumentView(long id, long studentId, long documentTypeId, String documentTypeName,
            String status, String purpose, LocalDateTime requestedAt, LocalDateTime processedAt,
            String adminRemarks, boolean downloadable) {}
    public record FeeView(long id, long studentId, long semesterId, BigDecimal totalAmount,
            LocalDate dueDate, BigDecimal verifiedPaid, BigDecimal outstanding) {}
    public record FeeSummary(java.util.List<FeeView> fees, BigDecimal totalAmount,
            BigDecimal verifiedPaid, BigDecimal outstanding) {}
    public record PaymentView(long id, long feeRecordId, long studentId, BigDecimal amount,
            String mode, String transactionReference, String status, LocalDateTime paidAt,
            LocalDateTime verifiedAt, String receiptNo) {}
    public record Download(byte[] bytes, String contentType, String filename) {}
}
