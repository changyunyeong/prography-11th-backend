package com.prography.backend.domain.member.service;

import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberResponseDTO.MemberResultDTO login(MemberRequestDTO.LoginRequestDTO request) {

        // loginId로 회원 조회 → 없으면 LOGIN_FAILED
        // BCrypt 비밀번호 검증 → 불일치 시 LOGIN_FAILED
        Member member =  memberRepository.findByLoginId(request.getLoginId())
                .filter(m -> passwordEncoder.matches(request.getPassword(), m.getPassword()))
                .orElseThrow(
                        () -> new ApiException(ErrorCode.LOGIN_FAILED)
                );

        // 회원 상태가 WITHDRAWN이면 MEMBER_WITHDRAWN
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ApiException(ErrorCode.MEMBER_WITHDRAWN);
        }

        return MemberResponseDTO.MemberResultDTO.from(member);
    }

    public MemberResponseDTO.MemberResultDTO getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponseDTO.MemberResultDTO.from(member);
    }
}
