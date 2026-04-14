package com.spring.app.company.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.spring.app.company.domain.ApplicantDetailDTO;
import com.spring.app.company.domain.ApplicantListDTO;
import com.spring.app.company.domain.ImageFileDTO;
import com.spring.app.company.service.CompanyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Company - applicant API", description = "기업 지원자 관련 REST API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/company/applicant/api")
public class CompanyApplicantApiController {

    private final CompanyService service;

    // ====== 공통 상수 ======
    // 중복 문자열 리터럴을 상수로 분리해서 유지보수성을 높인다.
    private static final String STATUS_UNREAD = "UNREAD";
    private static final String STATUS_READ = "READ";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_INTERVIEW = "INTERVIEW";
    private static final String STATUS_PASSED = "PASSED";

    // 화면 경로도 문자열 중복을 줄이기 위해 상수화한다.
    private static final String VIEW_APPLICANT_DETAIL = "company/applicant/applicant_detail";
    private static final String REDIRECT_LOGIN = "redirect:/member/login";
    private static final String REDIRECT_APPLICANT_LIST = "redirect:/company/applicant/list";
    private static final String ACTIVE_MENU_APPLICANT = "applicant";

    // 메시지도 상수화하면 문구 수정 시 한 곳만 변경하면 된다.
    private static final String MESSAGE_STATUS_UPDATED = "상태가 변경되었습니다.";
    private static final String MESSAGE_STATUS_UPDATE_DENIED = "상세 확인 후 상태를 변경할 수 있습니다.";

    // 매직넘버를 제거하기 위해 페이지 크기도 상수로 분리한다.
    private static final int SIZE_PER_PAGE = 10;

    private String mapProcessStatus(Integer processStatus) {
        if (processStatus == null) {
            return STATUS_UNREAD;
        }

        switch (processStatus) {
            case 0:
                return STATUS_UNREAD;
            case 1:
                return STATUS_READ;
            case 2:
                return STATUS_REJECTED;   // 서류탈락
            case 3:
                return STATUS_INTERVIEW;  // 면접요청
            case 4:
                return STATUS_PASSED;     // 합격
            case 5:
                return STATUS_REJECTED;   // 불합격
            default:
                return STATUS_UNREAD;
        }
    }

    // 지원자 목록 조회(페이징 처리)
    @Operation(summary = "지원자 목록 조회", description = "기업 회원이 등록한 공고에 지원한 지원자 목록을 페이징 처리하여 조회한다.")
    @GetMapping("/list")
    public Map<String, Object> applicantList(Authentication authentication,
                                             @RequestParam(value = "keyword", required = false) String keyword,
                                             @RequestParam(value = "processStatus", required = false) Integer processStatus,
                                             @RequestParam(value = "jobId", required = false) Long jobId,
                                             @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo) {

        String memberId = authentication.getName();

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("memberId", memberId);
        paraMap.put("keyword", keyword);
        paraMap.put("processStatus", processStatus);
        paraMap.put("jobId", jobId);

        int totalCount = service.selectApplicantCount(paraMap);
        int totalPage = (int) Math.ceil((double) totalCount / SIZE_PER_PAGE);

        if (pageNo < 1) {
            pageNo = 1;
        }
        if (totalPage > 0 && pageNo > totalPage) {
            pageNo = totalPage;
        }

        int startRow = ((pageNo - 1) * SIZE_PER_PAGE) + 1;
        int endRow = startRow + SIZE_PER_PAGE - 1;

        paraMap.put("startRow", startRow);
        paraMap.put("endRow", endRow);

        List<ApplicantListDTO> list = service.selectApplicantListPaging(paraMap);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalCount", totalCount);
        result.put("sizePerPage", SIZE_PER_PAGE);
        result.put("currentShowPageNo", pageNo);
        result.put("totalPage", totalPage);

        return result;
    }

    // 지원자 상세 조회
    @Operation(summary = "지원자 이력서 조회", description = "기업 회원이 등록한 공고에 지원한 이력서를 조회한다.")
    @GetMapping("/detail")
    public ModelAndView applicantDetail(@RequestParam("applicationId") Long applicationId,
                                        Authentication authentication,
                                        ModelAndView mav) {

        if (authentication == null) {
            mav.setViewName(REDIRECT_LOGIN);
            return mav;
        }

        String companyMemberId = authentication.getName();

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("applicationId", applicationId);
        paraMap.put("memberId", companyMemberId);

        // 상세 진입 시 최초 열람 처리
        service.readApplicantDetail(paraMap);

        // 기업용 지원자 상세 조회
        ApplicantDetailDTO dto = service.getApplicantDetailForCompany(paraMap);

        if (dto == null) {
            mav.setViewName(REDIRECT_APPLICANT_LIST);
            return mav;
        }

        List<ImageFileDTO> fileList = service.getApplicationFiles(applicationId);
        Map<String, Object> appDetail = createApplicantDetailMap(dto, fileList);

        mav.addObject("appDetail", appDetail);
        mav.addObject("activeMenu", ACTIVE_MENU_APPLICANT);
        mav.setViewName(VIEW_APPLICANT_DETAIL);

        return mav;
    }

    // 지원자 상세 응답용 Map 생성 로직을 분리해서 메서드 가독성을 높인다.
    private Map<String, Object> createApplicantDetailMap(ApplicantDetailDTO dto, List<ImageFileDTO> fileList) {
        Map<String, Object> appDetail = new HashMap<>();

        appDetail.put("id", dto.getApplicationId());
        appDetail.put("postId", dto.getJobId());
        appDetail.put("postTitle", dto.getPostTitle());
        appDetail.put("companyName", dto.getCompanyName());
        appDetail.put("region", dto.getRegionName());
        appDetail.put("role", dto.getCategoryName());
        appDetail.put("status", mapProcessStatus(dto.getProcessStatus()));
        appDetail.put("statusText", dto.getProcessStatusText());
        appDetail.put("appliedAt", dto.getAppliedAt());
        appDetail.put("viewedAt", dto.getViewedAt());
        appDetail.put("resumeTitle", dto.getResumeTitle());
        appDetail.put("files", fileList);

        // tbl_job_application 스냅샷 정보
        appDetail.put("name", dto.getName());
        appDetail.put("birthDate", dto.getBirthDate());
        appDetail.put("gender", dto.getGender());
        appDetail.put("phone", dto.getPhone());
        appDetail.put("email", dto.getEmail());

        // tbl_submitted_resume 스냅샷 정보
        appDetail.put("selfIntro", dto.getSelfIntro());
        appDetail.put("education", dto.getEducation());
        appDetail.put("career", dto.getCareer());
        appDetail.put("language", dto.getLanguage());
        appDetail.put("portfolio", dto.getPortfolio());
        appDetail.put("award", dto.getAward());
        appDetail.put("address", dto.getAddress());
        appDetail.put("photoPath", dto.getPhotoPath());
        appDetail.put("desiredSalary", dto.getDesiredSalary());

        List<Map<String, Object>> techstackList = service.getApplicationTechstackList(dto.getSubmittedResumeId());
        List<Map<String, Object>> certificateList = service.getApplicationCertificateList(dto.getSubmittedResumeId());

        appDetail.put("techstacks", techstackList);
        appDetail.put("certificates", certificateList);

        return appDetail;
    }

    // 지원자 상태 변경
    @Operation(summary = "지원자 상태 변경", description = "기업 회원이 등록한 공고에 지원한 지원자 상태를 변경한다.")
    @PutMapping("/status")
    public Map<String, Object> updateApplicantStatus(@RequestBody Map<String, Object> request,
                                                     Authentication authentication) {

        String memberId = authentication.getName();

        Long applicationId = Long.valueOf(String.valueOf(request.get("applicationId")));
        Integer prevStatus = Integer.valueOf(String.valueOf(request.get("prevStatus")));
        Integer newStatus = Integer.valueOf(String.valueOf(request.get("newStatus")));

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("applicationId", applicationId);
        paraMap.put("prevStatus", prevStatus);
        paraMap.put("newStatus", newStatus);
        paraMap.put("memberId", memberId);

        boolean success = service.updateApplicantStatus(paraMap);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? MESSAGE_STATUS_UPDATED : MESSAGE_STATUS_UPDATE_DENIED);

        return result;
    }
}