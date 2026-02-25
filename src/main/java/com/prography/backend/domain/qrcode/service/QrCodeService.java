package com.prography.backend.domain.qrcode.service;

import com.prography.backend.domain.qrcode.dto.QrCodeResponseDTO;
import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.qrcode.repository.QrCodeRepository;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;

    public QrCodeResponseDTO.QrCodeRenewDTO renewQrCode(Long qrCodeId) {
        QrCode currentQrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ApiException(ErrorCode.QR_NOT_FOUND));

        Instant now = Instant.now();
        currentQrCode.expire(now); // 기존 QR 코드의 expiresAt을 현재 시각으로 설정 (즉시 만료)

        QrCode renewedQrCode = QrCode.builder()
                .session(currentQrCode.getSession())
                .hashValue(UUID.randomUUID().toString())
                .expiresAt(now.plus(24, ChronoUnit.HOURS)) // 동일 sessionId로 새 QR 코드 생성 (UUID hashValue, 24시간 유효)
                .build();
        renewedQrCode = qrCodeRepository.saveAndFlush(renewedQrCode);

        return QrCodeResponseDTO.QrCodeRenewDTO.from(renewedQrCode); // 새로 생성된 QR 코드 정보 반환
    }
}
