package com.spring.app.company.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spring.app.company.domain.TalentFilterResponseDTO;
import com.spring.app.company.domain.TalentResumeDetailDTO;
import com.spring.app.company.domain.TalentSearchConditionDTO;
import com.spring.app.company.service.CompanyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company/talent")
public class CompanyTalentApiController {

    private final CompanyService service;

    // 중복 문자열을 상수로 분리하면 유지보수 시 수정 포인트를 줄일 수 있다.
    private static final String KEY_RESUME_LIST = "resumeList";
    private static final String KEY_TOTAL_COUNT = "totalCount";

    // 인재검색 필터 데이터 조회
    @GetMapping("/filters")
    public ResponseEntity<TalentFilterResponseDTO> getTalentFilters() {

        TalentFilterResponseDTO dto = new TalentFilterResponseDTO();
        dto.setJobCategoryList(service.getJobCategoryList());
        dto.setSkillCategoryList(service.getSkillCategoryList());
        dto.setSkillList(service.getSkillList());

        return ResponseEntity.ok(dto);
    }

    // 공개 대표이력서 목록 조회
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getTalentList(@ModelAttribute TalentSearchConditionDTO searchDto) {

        Map<String, Object> result = new HashMap<>();
        result.put(KEY_RESUME_LIST, service.getPublicPrimaryResumeList(searchDto));
        result.put(KEY_TOTAL_COUNT, service.getPublicPrimaryResumeCount(searchDto));

        return ResponseEntity.ok(result);
    }

    // 공개 대표이력서 상세 조회
    @GetMapping("/detail")
    public ResponseEntity<TalentResumeDetailDTO> getTalentDetail(@RequestParam("resumeId") Long resumeId) {

        TalentResumeDetailDTO dto = service.getPublicPrimaryResumeDetail(resumeId);

        // wildcard 반환 타입(ResponseEntity<?>) 대신
        // 성공/실패 모두 TalentResumeDetailDTO 기반의 명확한 응답 타입으로 통일한다.
        // 조회 결과가 없을 때는 404(Not Found)로 반환하는 것이 의미상 더 자연스럽다.
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dto);
    }
}