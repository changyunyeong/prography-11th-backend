package com.prography.backend.domain.auth.service;

import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.auth.dto.AuthResponseDTO;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponseDTO.LoginResultDTO login(String loginId, String rawPassword) {
        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new ApiException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new ApiException(ErrorCode.LOGIN_FAILED);
        }

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ApiException(ErrorCode.MEMBER_WITHDRAWN);
        }

        return AuthResponseDTO.LoginResultDTO.from(member);
    }
}
