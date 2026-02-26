package com.prography.backend.support;

import com.prography.backend.domain.attendance.dto.AttendanceRequestDTO;
import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.domain.cohort.entity.Team;
import com.prography.backend.domain.deposit.entity.DepositHistory;
import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.session.dto.SessionRequestDTO;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.global.common.enums.AttendanceSource;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.DepositType;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.PartType;
import com.prography.backend.global.common.enums.SessionStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Cohort cohort(Long id, int generation, String name, boolean currentOperating) {
        Cohort cohort = Cohort.builder()
                .generation(generation)
                .name(name)
                .currentOperating(currentOperating)
                .build();
        return withId(cohort, id);
    }

    public static Member member(
            Long id,
            String loginId,
            String password,
            String name,
            String phone,
            MemberRole role,
            MemberStatus status
    ) {
        Member member = Member.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .phone(phone)
                .role(role)
                .status(status)
                .build();
        return withId(member, id);
    }

    public static Part part(Long id, Cohort cohort, PartType type) {
        Part part = Part.builder()
                .cohort(cohort)
                .type(type)
                .build();
        return withId(part, id);
    }

    public static Team team(Long id, Cohort cohort, String name) {
        Team team = Team.builder()
                .cohort(cohort)
                .name(name)
                .build();
        return withId(team, id);
    }

    public static CohortMember cohortMember(
            Long id,
            Cohort cohort,
            Member member,
            Part part,
            Team team,
            int depositBalance,
            int excuseCount
    ) {
        CohortMember cohortMember = CohortMember.builder()
                .cohort(cohort)
                .member(member)
                .part(part)
                .team(team)
                .depositBalance(depositBalance)
                .excuseCount(excuseCount)
                .build();
        return withId(cohortMember, id);
    }

    public static ClubSession session(
            Long id,
            Cohort cohort,
            String title,
            LocalDateTime startsAt,
            String location,
            SessionStatus status
    ) {
        ClubSession session = ClubSession.builder()
                .cohort(cohort)
                .title(title)
                .startsAt(startsAt)
                .location(location)
                .status(status)
                .build();
        return withId(session, id);
    }

    public static QrCode qrCode(Long id, ClubSession session, String hashValue, Instant expiresAt) {
        QrCode qrCode = QrCode.builder()
                .session(session)
                .hashValue(hashValue)
                .expiresAt(expiresAt)
                .build();
        return withId(qrCode, id);
    }

    public static Attendance attendance(
            Long id,
            ClubSession session,
            Member member,
            CohortMember cohortMember,
            QrCode qrCode,
            AttendanceStatus status,
            AttendanceSource source,
            Instant checkedAt,
            Integer lateMinutes,
            int penaltyAmount,
            String reason
    ) {
        Attendance attendance = Attendance.builder()
                .session(session)
                .member(member)
                .cohortMember(cohortMember)
                .qrCode(qrCode)
                .status(status)
                .source(source)
                .checkedAt(checkedAt)
                .latenessMinutes(lateMinutes)
                .penaltyAmount(penaltyAmount)
                .reason(reason)
                .build();
        return withId(attendance, id);
    }

    public static DepositHistory depositHistory(
            Long id,
            CohortMember cohortMember,
            DepositType type,
            int amount,
            int before,
            int after,
            Attendance attendance,
            String description
    ) {
        DepositHistory history = DepositHistory.builder()
                .cohortMember(cohortMember)
                .type(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .attendance(attendance)
                .description(description)
                .build();
        return withId(history, id);
    }

    public static MemberRequestDTO.LoginRequestDTO loginRequest(String loginId, String password) {
        MemberRequestDTO.LoginRequestDTO request = new MemberRequestDTO.LoginRequestDTO();
        setField(request, "loginId", loginId);
        setField(request, "password", password);
        return request;
    }

    public static MemberRequestDTO.CreateMemberRequestDTO createMemberRequest(
            String loginId,
            String password,
            String name,
            String phone,
            Long cohortId,
            Long partId,
            Long teamId
    ) {
        MemberRequestDTO.CreateMemberRequestDTO request = new MemberRequestDTO.CreateMemberRequestDTO();
        setField(request, "loginId", loginId);
        setField(request, "password", password);
        setField(request, "name", name);
        setField(request, "phone", phone);
        setField(request, "cohortId", cohortId);
        setField(request, "partId", partId);
        setField(request, "teamId", teamId);
        return request;
    }

    public static MemberRequestDTO.UpdateMemberRequestDTO updateMemberRequest(
            String name,
            String phone,
            Long cohortId,
            Long partId,
            Long teamId
    ) {
        MemberRequestDTO.UpdateMemberRequestDTO request = new MemberRequestDTO.UpdateMemberRequestDTO();
        setField(request, "name", name);
        setField(request, "phone", phone);
        setField(request, "cohortId", cohortId);
        setField(request, "partId", partId);
        setField(request, "teamId", teamId);
        return request;
    }

    public static SessionRequestDTO.CreateSessionRequestDTO createSessionRequest(
            String title,
            LocalDate date,
            String time,
            String location
    ) {
        SessionRequestDTO.CreateSessionRequestDTO request = new SessionRequestDTO.CreateSessionRequestDTO();
        setField(request, "title", title);
        setField(request, "date", date);
        setField(request, "time", time);
        setField(request, "location", location);
        return request;
    }

    public static SessionRequestDTO.UpdateSessionRequestDTO updateSessionRequest(
            String title,
            LocalDate date,
            String time,
            String location,
            SessionStatus status
    ) {
        SessionRequestDTO.UpdateSessionRequestDTO request = new SessionRequestDTO.UpdateSessionRequestDTO();
        setField(request, "title", title);
        setField(request, "date", date);
        setField(request, "time", time);
        setField(request, "location", location);
        setField(request, "status", status);
        return request;
    }

    public static AttendanceRequestDTO.CheckAttendanceRequestDTO checkAttendanceRequest(String hashValue, Long memberId) {
        AttendanceRequestDTO.CheckAttendanceRequestDTO request = new AttendanceRequestDTO.CheckAttendanceRequestDTO();
        setField(request, "hashValue", hashValue);
        setField(request, "memberId", memberId);
        return request;
    }

    public static AttendanceRequestDTO.RegisterAttendanceRequestDTO registerAttendanceRequest(
            Long sessionId,
            Long memberId,
            AttendanceStatus status,
            Integer lateMinutes,
            String reason
    ) {
        AttendanceRequestDTO.RegisterAttendanceRequestDTO request = new AttendanceRequestDTO.RegisterAttendanceRequestDTO();
        setField(request, "sessionId", sessionId);
        setField(request, "memberId", memberId);
        setField(request, "status", status);
        setField(request, "lateMinutes", lateMinutes);
        setField(request, "reason", reason);
        return request;
    }

    public static AttendanceRequestDTO.UpdateAttendanceRequestDTO updateAttendanceRequest(
            AttendanceStatus status,
            Integer lateMinutes,
            String reason
    ) {
        AttendanceRequestDTO.UpdateAttendanceRequestDTO request = new AttendanceRequestDTO.UpdateAttendanceRequestDTO();
        setField(request, "status", status);
        setField(request, "lateMinutes", lateMinutes);
        setField(request, "reason", reason);
        return request;
    }

    private static void setField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    private static <T> T withId(T target, Long id) {
        if (id != null) {
            ReflectionTestUtils.setField(target, "id", id);
        }
        return target;
    }
}
