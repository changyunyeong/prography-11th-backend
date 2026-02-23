package com.prography.backend.domain.qrcode.controller;

import com.prography.backend.domain.qrcode.dto.QrCodeResponseDTO;
import com.prography.backend.domain.qrcode.service.QrCodeService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "QR codes", description = "QR codes API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/qrcodes")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @PutMapping("/{qrCodeId}")
    @Operation(summary = "QR 코드 갱신", description = "기존 QR 코드를 즉시 만료시키고, 동일 일정에 새 QR 코드를 생성합니다.")
    public ApiResponse<QrCodeResponseDTO.QrCodeRenewDTO> renewQrCode(@PathVariable("qrCodeId") Long qrCodeId) {
        QrCodeResponseDTO.QrCodeRenewDTO response = qrCodeService.renewQrCode(qrCodeId);
        return ApiResponse.success(response);

    }
}

