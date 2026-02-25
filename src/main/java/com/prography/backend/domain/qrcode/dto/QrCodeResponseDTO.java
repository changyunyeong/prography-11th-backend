package com.prography.backend.domain.qrcode.dto;

import com.prography.backend.domain.qrcode.entity.QrCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class QrCodeResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QrCodeRenewDTO {
        private Long id;
        private Long sessionId;
        private String hashValue;
        private Instant createdAt;
        private Instant expiresAt;

        public static QrCodeResponseDTO.QrCodeRenewDTO from(QrCode qrCode) {
            return QrCodeResponseDTO.QrCodeRenewDTO.builder()
                    .id(qrCode.getId())
                    .sessionId(qrCode.getSession().getId())
                    .hashValue(qrCode.getHashValue())
                    .createdAt(qrCode.getCreatedAt())
                    .expiresAt(qrCode.getExpiresAt())
                    .build();
        }
    }
}
