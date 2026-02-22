package com.prography.backend.domain.member.service;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.domain.cohort.entity.Team;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.cohort.repository.PartRepository;
import com.prography.backend.domain.cohort.repository.TeamRepository;
import com.prography.backend.domain.deposit.service.DepositService;
import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private static final int INITIAL_DEPOSIT = 100_000;
    private static final String SEARCH_TYPE_NAME = "name";
    private static final String SEARCH_TYPE_LOGIN_ID = "loginid";
    private static final String SEARCH_TYPE_PHONE = "phone";
    private static final Set<String> ALLOWED_SEARCH_TYPES = Set.of(
            SEARCH_TYPE_NAME, SEARCH_TYPE_LOGIN_ID, SEARCH_TYPE_PHONE
    );

    private final MemberRepository memberRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepositService depositService;
    private final CurrentCohortProvider currentCohortProvider;

    public MemberResponseDTO.MemberResultDTO createMember(MemberRequestDTO.CreateMemberRequestDTO request) {

        // loginId 중복 검사
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // cohortId, partId, teamId 존재 검증
        Cohort cohort = currentCohortProvider.getCurrentCohort();
        if (request.getCohortId() != null && !request.getCohortId().equals(cohort.getId())) {
            throw new ApiException(ErrorCode.COHORT_NOT_FOUND);
        }

        Part part = null;
        Long partId = request.getPartId();
        if (partId != null) {
            part = partRepository.findById(partId)
                    .filter(value -> value.getCohort().getId().equals(cohort.getId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.PART_NOT_FOUND));
        }

        Team team = null;
        Long teamId = request.getTeamId();
        if (teamId != null) {
            team = teamRepository.findById(teamId)
                    .filter(value -> value.getCohort().getId().equals(cohort.getId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.TEAM_NOT_FOUND));
        }

        // 비밀번호 BCrypt 해싱 (cost factor 12) && Member 생성 (status=ACTIVE, role=MEMBER)
        Member member = Member.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.MEMBER)
                .build();
        member = memberRepository.save(member);

        // CohortMember 생성 (deposit=100,000원)
        CohortMember cohortMember = CohortMember.builder()
                .member(member)
                .cohort(cohort)
                .part(part)
                .team(team)
                .depositBalance(0)
                .build();
        cohortMemberRepository.save(cohortMember);

        // DepositHistory 생성 (type=INITIAL, amount=100,000원)
        depositService.initializeDeposit(cohortMember, INITIAL_DEPOSIT, "초기 보증금 설정");

        return MemberResponseDTO.MemberResultDTO.from(member, cohortMember);
    }

    @Transactional(readOnly = true)
    public MemberResponseDTO.MemberDashboardPreViewListDTO getMemberDashboard(
            int page,
            int size,
            String searchType,
            String searchValue,
            Integer generation,
            String partName,
            String teamName,
            MemberStatus status
    ) {
        validateDashboardRequest(page, size, searchType, searchValue);

        String normalizedSearchType = normalize(searchType);
        String normalizedSearchValue = normalize(searchValue);
        String normalizedPartName = normalize(partName);
        String normalizedTeamName = normalize(teamName);
        boolean hasCohortFilter = generation != null || normalizedPartName != null || normalizedTeamName != null;

        Specification<Member> specification = buildDashboardSpecification(status, normalizedSearchType, normalizedSearchValue);
        List<Member> dbFilteredMembers = memberRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "id")); // 반환값 sql로 변환해 실행
        if (dbFilteredMembers.isEmpty()) {
            return MemberResponseDTO.MemberDashboardPreViewListDTO.builder()
                    .content(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(0L)
                    .totalPages(0)
                    .build();
        }

        Map<Long, List<CohortMember>> cohortMembersByMemberId = findCohortMembersByMemberId(dbFilteredMembers);
        Long currentCohortId = currentCohortProvider.getCurrentCohort().getId();

        List<MemberResponseDTO.MemberDashboardPreViewDTO> filteredMembers = new ArrayList<>();
        for (Member member : dbFilteredMembers) {
            List<CohortMember> cohortMembers = cohortMembersByMemberId.getOrDefault(member.getId(), List.of());

            if (hasCohortFilter) {
                CohortMember matchedCohortMember = findMatchedCohortMember(
                        cohortMembers, generation, normalizedPartName, normalizedTeamName, currentCohortId
                );
                if (matchedCohortMember == null) {
                    continue;
                }
                filteredMembers.add(MemberResponseDTO.MemberDashboardPreViewDTO.from(member, matchedCohortMember));
                continue;
            }

            CohortMember preferredCohortMember = choosePreferredCohortMember(cohortMembers, currentCohortId);
            filteredMembers.add(MemberResponseDTO.MemberDashboardPreViewDTO.from(member, preferredCohortMember));
        }

        int totalElements = filteredMembers.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        long offset = (long) page * size;
        int fromIndex = (int) Math.min(offset, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<MemberResponseDTO.MemberDashboardPreViewDTO> pageContent =
                fromIndex >= toIndex ? List.of() : filteredMembers.subList(fromIndex, toIndex);

        return MemberResponseDTO.MemberDashboardPreViewListDTO.builder()
                .content(pageContent)
                .page(page)
                .size(size)
                .totalElements((long) totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Transactional(readOnly = true)
    public MemberResponseDTO.MemberResultDTO getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        CohortMember cohortMember = cohortMemberRepository.findByCohortIdAndMemberId(
                        currentCohortProvider.getCurrentCohort().getId(), member.getId())
                .orElse(null);

        return MemberResponseDTO.MemberResultDTO.from(member, cohortMember);
    }

    private void validateDashboardRequest(int page, int size, String searchType, String searchValue) {
        if (page < 0 || size <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        boolean hasSearchType = normalize(searchType) != null;
        boolean hasSearchValue = normalize(searchValue) != null;
        if (hasSearchType != hasSearchValue) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        if (!hasSearchType) {
            return;
        }

        String normalizedSearchType = normalize(searchType);
        if (!ALLOWED_SEARCH_TYPES.contains(normalizedSearchType)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Specification<Member> buildDashboardSpecification(
            MemberStatus status,
            String searchType,
            String searchValue
    ) { // criteriaBuilder: 조건 생성기 객체
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>(); // 조건 누적

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (searchType != null || searchValue != null) { // searchType/searchValue가 있으면 LIKE 조건 추가
                String likeValue = "%" + searchValue + "%";
                switch (searchType) {
                    case SEARCH_TYPE_NAME -> predicates.add(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likeValue)
                    );
                    case SEARCH_TYPE_LOGIN_ID -> predicates.add(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("loginId")), likeValue)
                    );
                    case SEARCH_TYPE_PHONE -> predicates.add(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), likeValue)
                    );
                    default -> throw new ApiException(ErrorCode.INVALID_REQUEST);
                }
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Map<Long, List<CohortMember>> findCohortMembersByMemberId(List<Member> members) {
        List<Long> memberIds = members.stream()
                .map(Member::getId)
                .toList();

        if (memberIds.isEmpty()) {
            return Map.of();
        }

        List<CohortMember> cohortMembers = cohortMemberRepository.findAllByMemberIdIn(memberIds);
        Map<Long, List<CohortMember>> map = new HashMap<>();

        for (CohortMember cohortMember : cohortMembers) {
            Long memberId = cohortMember.getMember().getId();
            map.computeIfAbsent(memberId, ignored -> new ArrayList<>()).add(cohortMember);
        } //  memberId가 map에 처음 등장하면 새 ArrayList를 만들어 넣고, 이미 있으면 기존 리스트를 반환한 뒤, 거기에 cohortMember를 add

        return map;
    }

    private CohortMember findMatchedCohortMember(
            List<CohortMember> cohortMembers,
            Integer generation,
            String partName,
            String teamName,
            Long currentCohortId
    ) {
        if (cohortMembers.isEmpty()) {
            return null;
        }

        for (CohortMember cohortMember : cohortMembers) {
            if (Objects.equals(cohortMember.getCohort().getId(), currentCohortId)
                    && matchesCohortFilter(cohortMember, generation, partName, teamName)) {
                return cohortMember;
            }
        }

        for (CohortMember cohortMember : cohortMembers) {
            if (matchesCohortFilter(cohortMember, generation, partName, teamName)) {
                return cohortMember;
            }
        }

        return null;
    }

    private CohortMember choosePreferredCohortMember(List<CohortMember> cohortMembers, Long currentCohortId) {
        if (cohortMembers.isEmpty()) {
            return null;
        }

        for (CohortMember cohortMember : cohortMembers) {
            if (Objects.equals(cohortMember.getCohort().getId(), currentCohortId)) {
                return cohortMember;
            }
        }

        return cohortMembers.stream()
                .max(Comparator.comparingInt(this::extractGeneration))
                .orElse(null);
    }

    private boolean matchesCohortFilter(CohortMember cohortMember, Integer generation, String partName, String teamName) {
        if (generation != null && !generation.equals(cohortMember.getCohort().getGeneration())) {
            return false;
        }

        if (partName != null) {
            String currentPartName = cohortMember.getPart() == null ? null : cohortMember.getPart().getType().name();
            if (currentPartName == null || !currentPartName.equalsIgnoreCase(partName)) {
                return false;
            }
        }

        if (teamName != null) {
            String currentTeamName = cohortMember.getTeam() == null ? null : cohortMember.getTeam().getName();
            if (currentTeamName == null || !currentTeamName.equalsIgnoreCase(teamName)) {
                return false;
            }
        }

        return true;
    }

    private int extractGeneration(CohortMember cohortMember) {
        Integer generation = cohortMember.getCohort() == null ? null : cohortMember.getCohort().getGeneration();
        return generation == null ? Integer.MIN_VALUE : generation;
    }

    // 대소문자 무시 및 공백/빈값을 null로 통일
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim(); // 공백 제거
        if (trimmedValue.isEmpty()) {
            return null;
        }
        return trimmedValue.toLowerCase(java.util.Locale.ROOT);
    }
}
