package com.prography.backend.domain.qrcode.service;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.qrcode.dto.QrCodeResponseDTO;
import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.qrcode.repository.QrCodeRepository;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.prography.backend.global.support.TestFixtures.cohort;
import static com.prography.backend.global.support.TestFixtures.qrCode;
import static com.prography.backend.global.support.TestFixtures.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @Mock
    private QrCodeRepository qrCodeRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenAnswer(ignored -> Instant.now());
    }

    @Test
    void QR갱신시_기존QR만료후_새QR을_생성한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "정기모임", LocalDateTime.now(), "강남", SessionStatus.SCHEDULED);
        QrCode oldQr = qrCode(10L, session, "old-hash", Instant.now().plus(1, ChronoUnit.HOURS));

        // when
        when(qrCodeRepository.findById(10L)).thenReturn(Optional.of(oldQr));
        when(qrCodeRepository.saveAndFlush(any(QrCode.class))).thenAnswer(invocation -> {
            QrCode newQr = invocation.getArgument(0, QrCode.class);
            ReflectionTestUtils.setField(newQr, "id", 11L);
            return newQr;
        });

        QrCodeResponseDTO.QrCodeRenewDTO result = qrCodeService.renewQrCode(10L);

        // then
        assertThat(oldQr.getExpiresAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getHashValue()).isNotEqualTo("old-hash");
        assertThat(result.getExpiresAt()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));
    }

    @Test
    void QR갱신시_QR없으면_QR_NOT_FOUND_예외() {

        when(qrCodeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCodeService.renewQrCode(404L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QR_NOT_FOUND);
    }
}
