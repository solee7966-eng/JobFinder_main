package com.spring.app.company.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spring.app.common.domain.JobCategoryDTO;
import com.spring.app.common.domain.RegionDTO;
import com.spring.app.company.domain.JobPostingDTO;
import com.spring.app.company.domain.JobPostingEditResponseDTO;
import com.spring.app.company.domain.JobPostingUpdateRequestDTO;
import com.spring.app.company.service.CompanyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * Company - Job REST API (AJAX/Swagger)
 */
@Tag(name = "Company - Job API", description = "기업 채용공고 관련 REST API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/company/api")
public class CompanyJobApiController {

    private final CompanyService service;

    // 중복 문자열 리터럴을 상수로 분리해 유지보수성을 높인다.
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DATA = "data";

    // 반복되는 응답 메시지도 상수화해서 한 곳에서 관리한다.
    private static final String MSG_NOT_FOUND = "존재하지 않는 공고입니다.";
    private static final String MSG_FORBIDDEN_EDIT = "본인 공고만 수정할 수 있습니다.";
    private static final String MSG_FORBIDDEN_DELETE = "본인 공고만 삭제할 수 있습니다.";
    private static final String MSG_HIDDEN_JOB = "신고된 공고는 수정할 수 없습니다.";
    private static final String MSG_UPDATE_SUCCESS = "수정되었습니다.";
    private static final String MSG_UPDATE_FAIL = "수정에 실패했습니다.";
    private static final String MSG_DELETE_SUCCESS = "삭제되었습니다.";
    private static final String MSG_DELETE_FAIL = "삭제에 실패했습니다.";

    // ====== Lookups (job category / region) ======
    @Operation(summary = "직무 중분류 목록 조회", description = "대분류(parentId)에 속한 중분류 목록을 조회한다.")
    @GetMapping("/job/categories/children")
    public List<JobCategoryDTO> jobCategoryChildren(@RequestParam("parentId") @NotNull Long parentId) {
        return service.getChildren(parentId);
    }

    @Operation(summary = "지역 하위 목록 조회", description = "상위 지역 코드(parentCode)에 속한 하위 지역 목록을 조회한다.")
    @GetMapping("/region/children")
    public List<RegionDTO> regionChildren(@RequestParam("parentCode") @NotBlank String parentCode) {
        return service.getRegionChildren(parentCode);
    }

    // ====== Job CRUD ======
    @Operation(summary = "채용공고 1건 조회", description = "jobId로 채용공고 상세를 조회한다. (수정 화면 prefill 등에 사용)")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobPostingDTO> getJob(@PathVariable("jobId") long jobId) {
        JobPostingDTO dto = service.getJobPostingOne(jobId);

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dto);
    }

    // 채용공고 등록하기
    @Operation(summary = "채용공고 등록", description = "채용공고를 등록한다.")
    @PostMapping("/jobs")
    public ResponseEntity<Map<String, Object>> createJob(
            @RequestBody JobPostingUpdateRequestDTO req,
            Authentication authentication) {

        JobPostingDTO dto = req.getJob();

        // 로그인한 기업 회원 아이디를 DTO에 세팅한다.
        String loginMemberId = authentication.getName();
        dto.setMemberId(loginMemberId);

        // 직무 및 기술스택 매핑까지 포함하여 채용공고를 등록한다.
        int n = service.insertJobPosting(dto, req.getSkillIds());

        // 성공 여부를 공통 응답 구조로 내려준다.
        return ResponseEntity.ok(createResponseMap(n == 1, null));
    }

    // 채용공고 수정을 위한 기존 데이터 불러오기
    @Operation(summary = "기존 채용공고 데이터 조회", description = "기존 jobId의 채용공고 데이터를 조회한다.")
    @GetMapping("/jobs/{jobId}/edit")
    public ResponseEntity<Map<String, Object>> getForEdit(@PathVariable("jobId") long jobId,
                                                          Authentication authentication) {

        String loginMemberId = authentication.getName();
        JobPostingDTO origin = service.getJobPostingOne(jobId);

        if (origin == null) {
            return ResponseEntity.status(404)
                    .body(createResponseMap(false, MSG_NOT_FOUND));
        }

        if (!loginMemberId.equals(origin.getMemberId())) {
            return ResponseEntity.status(403)
                    .body(createResponseMap(false, MSG_FORBIDDEN_EDIT));
        }

        if (origin.getIsHidden() != null && origin.getIsHidden() == 1) {
            return ResponseEntity.status(409)
                    .body(createResponseMap(false, MSG_HIDDEN_JOB));
        }

        JobPostingEditResponseDTO dto = service.getJobPostingForEdit(jobId);

        // wildcard(ResponseEntity<?>)를 쓰지 않고,
        // success + data 구조의 명확한 Map 응답으로 통일한다.
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(KEY_SUCCESS, true);
        resultMap.put(KEY_DATA, dto);

        return ResponseEntity.ok(resultMap);
    }

    // 변경된 공고 내용을 수정해주기(인증/소유자/신고체크)
    @Operation(summary = "채용공고 수정", description = "jobId의 채용공고를 수정한다.")
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> updateJob(
            @PathVariable("jobId") long jobId,
            @RequestBody JobPostingUpdateRequestDTO req,
            Authentication authentication) {

        String loginMemberId = authentication.getName();
        JobPostingDTO origin = service.getJobPostingOne(jobId);

        if (origin == null) {
            return ResponseEntity.status(404)
                    .body(createResponseMap(false, MSG_NOT_FOUND));
        }

        if (!loginMemberId.equals(origin.getMemberId())) {
            return ResponseEntity.status(403)
                    .body(createResponseMap(false, MSG_FORBIDDEN_EDIT));
        }

        if (origin.getIsHidden() != null && origin.getIsHidden() == 1) {
            return ResponseEntity.status(409)
                    .body(createResponseMap(false, MSG_HIDDEN_JOB));
        }

        JobPostingDTO dto = req.getJob();
        dto.setJobId(jobId);
        dto.setMemberId(loginMemberId);

        int n = service.updateJobPosting(dto, req.getSkillIds());

        return ResponseEntity.ok(
                createResponseMap(n == 1, n == 1 ? MSG_UPDATE_SUCCESS : MSG_UPDATE_FAIL)
        );
    }

    // 공고 삭제하기
    @Operation(summary = "채용공고 삭제", description = "jobId의 채용공고를 삭제(소프트삭제)한다.")
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable("jobId") Long jobId,
                                                         Authentication authentication) {

        String loginMemberId = authentication.getName();
        JobPostingDTO origin = service.getJobPostingOne(jobId);

        if (origin == null) {
            return ResponseEntity.status(404)
                    .body(createResponseMap(false, MSG_NOT_FOUND));
        }

        if (!loginMemberId.equals(origin.getMemberId())) {
            return ResponseEntity.status(403)
                    .body(createResponseMap(false, MSG_FORBIDDEN_DELETE));
        }

        int n = service.deleteJobPosting(jobId, loginMemberId);

        return ResponseEntity.ok(
                createResponseMap(n == 1, n == 1 ? MSG_DELETE_SUCCESS : MSG_DELETE_FAIL)
        );
    }

    /**
     * success/message 형태의 공통 응답 Map 생성 메서드.
     * 동일한 Map.of("success", ..., "message", ...) 반복을 줄이기 위해 사용한다.
     */
    private Map<String, Object> createResponseMap(boolean success, String message) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(KEY_SUCCESS, success);

        // message가 필요한 경우에만 내려주도록 처리한다.
        if (message != null) {
            resultMap.put(KEY_MESSAGE, message);
        }

        return resultMap;
    }
}