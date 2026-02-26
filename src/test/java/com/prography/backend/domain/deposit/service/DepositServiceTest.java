package com.prography.backend.domain.deposit.service;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.deposit.entity.DepositHistory;
import com.prography.backend.domain.deposit.repository.DepositHistoryRepository;
import com.prography.backend.global.common.enums.AttendanceSource;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.DepositType;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static com.prography.backend.support.TestFixtures.attendance;
import static com.prography.backend.support.TestFixtures.cohort;
import static com.prography.backend.support.TestFixtures.cohortMember;
import static com.prography.backend.support.TestFixtures.member;
import static com.prography.backend.support.TestFixtures.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock
    private DepositHistoryRepository depositHistoryRepository;

    @InjectMocks
    private DepositService depositService;

    @Test
    void 보증금초기화시_잔액증가와_이력기록() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        var member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 0, 0);

        // when
        depositService.initializeDeposit(cohortMember, 100_000, "초기 보증금");

        // then
        assertThat(cohortMember.getDepositBalance()).isEqualTo(100_000);

        ArgumentCaptor<DepositHistory> captor = ArgumentCaptor.forClass(DepositHistory.class);
        verify(depositHistoryRepository).save(captor.capture());
        DepositHistory history = captor.getValue();
        assertThat(history.getType()).isEqualTo(DepositType.INITIAL);
        assertThat(history.getAmount()).isEqualTo(100_000);
        assertThat(history.getBalanceBefore()).isEqualTo(0);
        assertThat(history.getBalanceAfter()).isEqualTo(100_000);
    }

    @Test
    void 패널티적용시_잔액감소와_이력기록() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        var member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 100_000, 0);
        var session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Attendance attendance = attendance(
                1L, session, member, cohortMember, null, AttendanceStatus.ABSENT, AttendanceSource.ADMIN, null, null, 10_000, "결석"
        );

        // when
        depositService.applyPenalty(cohortMember, 10_000, attendance, "패널티 차감");

        // then
        assertThat(cohortMember.getDepositBalance()).isEqualTo(90_000);
        ArgumentCaptor<DepositHistory> captor = ArgumentCaptor.forClass(DepositHistory.class);
        verify(depositHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(DepositType.PENALTY);
    }

    @Test
    void 패널티적용시_잔액부족이면_DEPOSIT_INSUFFICIENT_예외() {

        Cohort cohort = cohort(2L, 11, "11기", true);
        var member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 1_000, 0);


        assertThatThrownBy(() -> depositService.applyPenalty(cohortMember, 10_000, null, "패널티"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPOSIT_INSUFFICIENT);
    }

    @Test
    void 환급적용시_잔액증가와_이력기록() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        var member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 90_000, 0);

        // when
        depositService.applyRefund(cohortMember, 5_000, null, "환급");

        // then
        assertThat(cohortMember.getDepositBalance()).isEqualTo(95_000);
        ArgumentCaptor<DepositHistory> captor = ArgumentCaptor.forClass(DepositHistory.class);
        verify(depositHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(DepositType.REFUND);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(95_000);
    }
}
