package com.spring.app.company.controller.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.spring.app.company.domain.BannerDTO;
import com.spring.app.company.domain.BannerListDTO;
import com.spring.app.company.domain.JobPostingDTO;
import com.spring.app.company.service.CompanyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Company - Banner REST API (AJAX/Swagger)
 */
@Tag(name = "Company - Banner API", description = "기업 배너 관련 REST API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/company/api/banner")
public class CompanyBannerApiController {

    private final CompanyService service;

    // =========================
    // SonarQube 대응용 공통 상수
    // =========================
    // 중복 문자열 리터럴을 상수화하여 유지보수성을 높인다.
    private static final String KEY_RESULT = "result";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_SUCCESS = "success";

    // 인증이 없는 테스트 상황에서 사용할 기본 회원 아이디
    private static final String DEFAULT_TEST_COMPANY = "test_company";

    // 공통 응답 문구도 상수화하여 여러 곳에서 재사용한다.
    private static final String MSG_JOB_LIST_ERROR = "공고 목록 조회 중 오류가 발생했습니다.";
    private static final String MSG_BANNER_TITLE_REQUIRED = "배너 제목을 입력해 주세요.";
    private static final String MSG_BANNER_JOB_REQUIRED = "연결할 공고를 선택해 주세요.";
    private static final String MSG_BANNER_START_AT_REQUIRED = "시작일을 선택해 주세요.";
    private static final String MSG_BANNER_IMAGE_REQUIRED = "배너 이미지를 첨부해 주세요.";
    private static final String MSG_BANNER_INSERT_SUCCESS = "배너가 등록되었습니다.";
    private static final String MSG_BANNER_INSERT_ERROR = "배너 등록 중 오류가 발생했습니다.";
    private static final String MSG_BANNER_LIST_ERROR = "배너 목록 조회 중 오류가 발생했습니다.";
    private static final String MSG_BANNER_DELETE_SUCCESS = "배너가 삭제되었습니다.";
    private static final String MSG_BANNER_DELETE_DENIED = "삭제 가능한 배너가 아니거나 권한이 없습니다.";
    private static final String MSG_BANNER_DELETE_ERROR = "배너 삭제 중 오류가 발생했습니다.";
    private static final String MSG_PAYMENT_INFO_ERROR = "배너 결제 정보 조회 중 오류가 발생했습니다.";

    // 매직 넘버도 상수화하면 의미가 명확해진다.
    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_FAIL = 0;
    private static final int SIZE_PER_PAGE = 5;
    private static final int MIN_START_DATE_OFFSET = 2;

    //배너를 등록하기 위한 게시중인 공고 조회
    @Operation(summary = "배너 등록용 공고 목록 조회", description = "로그인한 기업의 공고 목록을 반환한다.")
    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getMyJobPostingList(Authentication authentication) {

        Map<String, Object> resultMap = new HashMap<>();

        try {
            String memberId = getMemberIdOrDefault(authentication);
            List<JobPostingDTO> jobList = service.getBannerPostingList(memberId);

            resultMap.put(KEY_RESULT, RESULT_SUCCESS);
            resultMap.put("jobList", jobList);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put(KEY_RESULT, RESULT_FAIL);
            resultMap.put(KEY_MESSAGE, MSG_JOB_LIST_ERROR);
        }

        return ResponseEntity.ok(resultMap);
    }

    //배너 등록하기
    @Operation(summary = "배너 등록", description = "배너 정보와 이미지를 함께 등록한다.")
    @PostMapping
    public ResponseEntity<Map<String, Object>> insertBanner(
            @ModelAttribute BannerDTO bannerDto,
            @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
            Authentication authentication) {

        Map<String, Object> resultMap = new HashMap<>();

        try {
            String memberId = getMemberIdOrDefault(authentication);
            bannerDto.setFkMemberId(memberId);

            // 유효성 검증 실패 시 공통 메서드로 바로 응답을 반환한다.
            String validationMessage = validateBannerInput(bannerDto, bannerImage);
            if (validationMessage != null) {
                return ResponseEntity.ok(createResultMap(RESULT_FAIL, validationMessage));
            }

            // 날짜 문자열 파싱도 예외 가능성이 있으므로 안전하게 처리한다.
            LocalDate minStartDate = LocalDate.now().plusDays(MIN_START_DATE_OFFSET);
            LocalDate startDate = LocalDate.parse(bannerDto.getStartAt());

            if (startDate.isBefore(minStartDate)) {
                return ResponseEntity.ok(
                        createResultMap(RESULT_FAIL, "배너 등록 시작일은 " + minStartDate + "부터 선택할 수 있습니다.")
                );
            }

            service.insertBannerWithImage(bannerDto, bannerImage);

            resultMap.put(KEY_RESULT, RESULT_SUCCESS);
            resultMap.put(KEY_MESSAGE, MSG_BANNER_INSERT_SUCCESS);

        } catch (DateTimeParseException e) {
            e.printStackTrace();
            resultMap.put(KEY_RESULT, RESULT_FAIL);
            resultMap.put(KEY_MESSAGE, "시작일 형식이 올바르지 않습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put(KEY_RESULT, RESULT_FAIL);
            resultMap.put(KEY_MESSAGE, e.getMessage() != null ? e.getMessage() : MSG_BANNER_INSERT_ERROR);
        }

        return ResponseEntity.ok(resultMap);
    }

    //배너 목록 조회(페이징처리)
    @Operation(summary = "배너 목록 조회", description = "로그인한 기업의 모든 배너를 페이징 처리하여 조회한다.")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getBannerList(
            @RequestParam(value = "currentShowPageNo", required = false, defaultValue = "1") String currentShowPageNo,
            Authentication authentication) {

        Map<String, Object> resultMap = new HashMap<>();

        try {
            service.refreshBannerStatuses();

            String memberId = authentication.getName();
            int totalCount = service.getBannerCountByMemberId(memberId);
            int totalPage = (int) Math.ceil((double) totalCount / SIZE_PER_PAGE);

            // 중첩 try-catch 제거:
            // 페이지 번호 파싱 로직을 별도 메서드로 분리해서 가독성을 높인다.
            int pageNo = parsePageNo(currentShowPageNo, totalPage);

            int startRow = ((pageNo - 1) * SIZE_PER_PAGE) + 1;
            int endRow = startRow + SIZE_PER_PAGE - 1;

            Map<String, Object> paraMap = new HashMap<>();
            paraMap.put("memberId", memberId);
            paraMap.put("startRow", startRow);
            paraMap.put("endRow", endRow);

            List<BannerListDTO> bannerList = service.getBannerListByMemberIdPaging(paraMap);

            resultMap.put(KEY_SUCCESS, true);
            resultMap.put("bannerList", bannerList);
            resultMap.put("totalCount", totalCount);
            resultMap.put("sizePerPage", SIZE_PER_PAGE);
            resultMap.put("currentShowPageNo", pageNo);
            resultMap.put("totalPage", totalPage);

            return ResponseEntity.ok(resultMap);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put(KEY_SUCCESS, false);
            resultMap.put(KEY_MESSAGE, MSG_BANNER_LIST_ERROR);
            return ResponseEntity.internalServerError().body(resultMap);
        }
    }

    //배너 삭제하기
    @Operation(summary = "배너 삭제", description = "로그인한 기업의 배너를 삭제한다.")
    @DeleteMapping("/{bannerId}")
    public ResponseEntity<Map<String, Object>> deleteBanner(@PathVariable("bannerId") Long bannerId,
                                                            HttpServletRequest request) {

        Map<String, Object> resultMap = new HashMap<>();

        try {
            String memberId = request.getUserPrincipal().getName();
            boolean success = service.deleteBanner(bannerId, memberId);

            if (success) {
                resultMap.put(KEY_SUCCESS, true);
                resultMap.put(KEY_MESSAGE, MSG_BANNER_DELETE_SUCCESS);
                return ResponseEntity.ok(resultMap);
            }

            resultMap.put(KEY_SUCCESS, false);
            resultMap.put(KEY_MESSAGE, MSG_BANNER_DELETE_DENIED);
            return ResponseEntity.badRequest().body(resultMap);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put(KEY_SUCCESS, false);
            resultMap.put(KEY_MESSAGE, MSG_BANNER_DELETE_ERROR);
            return ResponseEntity.internalServerError().body(resultMap);
        }
    }

    //포인트 정보 조회
    @Operation(summary = "배너 결제 정보 조회", description = "로그인한 기업의 현재 포인트와 배너 광고비를 반환한다.")
    @GetMapping("/payment-info")
    public ResponseEntity<Map<String, Object>> getBannerPaymentInfo(Authentication authentication) {

        Map<String, Object> resultMap = new HashMap<>();

        try {
            String memberId = authentication != null ? authentication.getName() : null;
            Map<String, Object> paymentInfo = service.getBannerPaymentInfo(memberId);

            resultMap.put(KEY_RESULT, RESULT_SUCCESS);
            resultMap.put("paymentInfo", paymentInfo);

        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put(KEY_RESULT, RESULT_FAIL);
            resultMap.put(KEY_MESSAGE, MSG_PAYMENT_INFO_ERROR);
        }

        return ResponseEntity.ok(resultMap);
    }

    /**
     * 인증 객체가 없을 때 테스트용 기본 아이디를 반환한다.
     * 같은 삼항 연산식이 반복되는 문제를 줄이기 위한 공통 메서드다.
     */
    private String getMemberIdOrDefault(Authentication authentication) {
        return authentication != null ? authentication.getName() : DEFAULT_TEST_COMPANY;
    }

    /**
     * 배너 등록 입력값 검증 로직을 분리하여 insertBanner 메서드의 길이를 줄인다.
     * 검증 실패 시 사용자에게 보여줄 메시지를 반환하고, 정상일 경우 null을 반환한다.
     */
    private String validateBannerInput(BannerDTO bannerDto, MultipartFile bannerImage) {
        if (bannerDto.getFkJobId() == null) {
            return MSG_BANNER_JOB_REQUIRED;
        }

        if (bannerDto.getTitle() == null || bannerDto.getTitle().trim().isEmpty()) {
            return MSG_BANNER_TITLE_REQUIRED;
        }

        if (bannerDto.getStartAt() == null || bannerDto.getStartAt().trim().isEmpty()) {
            return MSG_BANNER_START_AT_REQUIRED;
        }

        if (bannerImage == null || bannerImage.isEmpty()) {
            return MSG_BANNER_IMAGE_REQUIRED;
        }

        return null;
    }

    /**
     * result/message 형태의 공통 응답 Map 생성 메서드다.
     * 동일한 put 코드 반복을 줄여 SonarQube 중복 이슈를 완화한다.
     */
    private Map<String, Object> createResultMap(int result, String message) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(KEY_RESULT, result);
        resultMap.put(KEY_MESSAGE, message);
        return resultMap;
    }

    /**
     * 페이지 번호 파싱 전용 메서드다.
     * 중첩 try-catch 대신 여기서 NumberFormatException 을 처리한다.
     */
    private int parsePageNo(String currentShowPageNo, int totalPage) {
        int pageNo = 1;

        try {
            pageNo = Integer.parseInt(currentShowPageNo);
        } catch (NumberFormatException e) {
            pageNo = 1;
        }

        if (pageNo < 1) {
            pageNo = 1;
        }

        if (totalPage == 0) {
            return 1;
        }

        if (pageNo > totalPage) {
            return totalPage;
        }

        return pageNo;
    }
}