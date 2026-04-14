package com.spring.app.company.controller.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.spring.app.common.domain.EducationDTO;
import com.spring.app.common.domain.JobCategoryDTO;
import com.spring.app.company.domain.BannerListDTO;
import com.spring.app.company.domain.CompanyDashboardDTO;
import com.spring.app.company.domain.CompanyProfileDTO;
import com.spring.app.company.domain.CompanyTopbarDTO;
import com.spring.app.company.domain.JobPostingDTO;
import com.spring.app.company.domain.MemberSimpleDTO;
import com.spring.app.company.domain.OfferListDTO;
import com.spring.app.company.domain.TalentResumeDetailDTO;
import com.spring.app.company.domain.TalentSearchConditionDTO;
import com.spring.app.company.service.CompanyService;
import com.spring.app.notification.domain.NotificationDTO;
import com.spring.app.notification.service.NotificationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * SSR(Thymeleaf) 전용 컨트롤러: 화면 렌더링/페이지 이동/폼 submit 담당
 * API(JSON/AJAX)는 com.spring.app.company.controller.api 패키지로 분리한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/company")
public class CompanyWebController {

    private final CompanyService service;
    private final NotificationService notificationService;

    // 포트원 결제를 위한 객체 주입
    @Value("${portone.impCode}")
    private String impCode;

    // ===== 공통 상수 =====
    // 중복 문자열 리터럴을 상수화하여 유지보수 시 수정 포인트를 줄인다.
    private static final String ATTR_ACTIVE_MENU = "activeMenu";
    private static final String ATTR_ACTIVE_TAB = "activeTab";
    private static final String ATTR_NOTIFICATION_LIST = "notificationList";
    private static final String ATTR_UNREAD_NOTIFICATION_COUNT = "unreadNotificationCount";

    private static final String MENU_JOB = "job";
    private static final String MENU_PROFILE = "profile";
    private static final String MENU_APPLICANT = "applicant";
    private static final String MENU_OFFER = "offer";
    private static final String MENU_BANNER = "banner";
    private static final String MENU_WALLET = "wallet";
    private static final String MENU_TALENT = "talent";

    private static final String VIEW_COMPANY_DASHBOARD = "company/company_dashboard";
    private static final String VIEW_PROFILE = "company/profile";
    private static final String VIEW_JOB_LIST = "company/job/job_list";
    private static final String VIEW_JOB_WRITE = "company/job/job_write";
    private static final String VIEW_JOB_UPDATE = "company/job/job_update";
    private static final String VIEW_JOB_DETAIL_POPUP = "company/job/job_detail_popup";
    private static final String VIEW_APPLICANT_LIST = "company/applicant/applicant_list";
    private static final String VIEW_OFFER_LIST = "company/offer/offer_list";
    private static final String VIEW_BANNER_LIST = "company/banner/banner_list";
    private static final String VIEW_BANNER_WRITE = "company/banner/banner_write";
    private static final String VIEW_WALLET = "company/wallet";
    private static final String VIEW_TALENT_SEARCH = "company/talent/talent_search";
    private static final String VIEW_TALENT_DETAIL = "company/talent/talent_detail";

    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_COMPANY_DASHBOARD = "redirect:/company/company_dashboard";
    private static final String REDIRECT_JOB_LIST = "redirect:/company/job/list";
    private static final String REDIRECT_TALENT = "redirect:/company/talent?menu=talent";

    private static final int SIZE_PER_PAGE_5 = 5;
    private static final int TALENT_PAGE_SIZE = 10;

    // 기업 상단바 조회(기업ID, 기업명, 이메일)
    @ModelAttribute
    public void addCompanyTopbarInfo(Model model, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            model.addAttribute(ATTR_NOTIFICATION_LIST, java.util.Collections.emptyList());
            model.addAttribute(ATTR_UNREAD_NOTIFICATION_COUNT, 0);
            return;
        }

        String memberId = authentication.getName();

        CompanyTopbarDTO topbarInfo = service.getCompanyTopbarInfo(memberId);

        if (topbarInfo == null) {
            model.addAttribute("loginCompanyName", "기업명 미설정");
            model.addAttribute("loginCompanyEmail", "이메일 미등록");
            model.addAttribute("loginCompanyInitial", "C");
        } else {
            String companyName = topbarInfo.getCompanyName();
            String email = topbarInfo.getEmail();

            model.addAttribute(
                    "loginCompanyName",
                    (companyName != null && !companyName.isBlank()) ? companyName : "기업명 미설정"
            );

            model.addAttribute(
                    "loginCompanyEmail",
                    (email != null && !email.isBlank()) ? email : "이메일 미등록"
            );

            String initial = "C";
            if (companyName != null && !companyName.isBlank()) {
                initial = companyName.substring(0, 1);
            }

            model.addAttribute("loginCompanyInitial", initial);
        }

        List<NotificationDTO> notificationList = notificationService.getMyNotifications(memberId);
        int unreadNotificationCount = notificationService.getUnreadNotificationCount(memberId);

        model.addAttribute(ATTR_NOTIFICATION_LIST, notificationList);
        model.addAttribute(ATTR_UNREAD_NOTIFICATION_COUNT, unreadNotificationCount);
    }

    // 기업용 페이지 기본
    @GetMapping({"", "/"})
    public String company() {
        return REDIRECT_COMPANY_DASHBOARD;
    }

    // 기업 대시보드 페이지
    @GetMapping("/company_dashboard")
    public String dashboard(@RequestParam(value = "menu", defaultValue = "company_dashboard") String menu,
                            Model model,
                            Authentication authentication) {

        model.addAttribute(ATTR_ACTIVE_MENU, menu);

        String memberId = authentication.getName();
        CompanyDashboardDTO dashboard = service.getCompanyDashboard(memberId);

        model.addAttribute("dashboard", dashboard);
        return VIEW_COMPANY_DASHBOARD;
    }

    // ============================== 프로필 ============================== //
    // 기업 프로필 페이지
    @GetMapping("/profile")
    public String profile(@RequestParam(value = "tab", defaultValue = "basic") String tab,
                          Model model,
                          Authentication authentication) {

        model.addAttribute(ATTR_ACTIVE_MENU, MENU_PROFILE);
        model.addAttribute(ATTR_ACTIVE_TAB, tab);

        String memberId = authentication.getName();
        CompanyProfileDTO profile = service.getCompanyProfile(memberId);
        model.addAttribute("profile", profile);

        return VIEW_PROFILE;
    }
    // ============================== 프로필 ============================== //

    // ============================== 채용공고 ============================== //
    // 채용공고 리스트 조회(페이징 처리)
    @GetMapping("/job/list")
    public String jobs(@RequestParam(value = "jobId", required = false) Long jobId,
                       @RequestParam(value = "currentShowPageNo", required = false, defaultValue = "1") String currentShowPageNo,
                       Model model,
                       Authentication authentication) {

        model.addAttribute(ATTR_ACTIVE_MENU, MENU_JOB);

        service.refreshJobPostingStatuses();

        String memberId = authentication.getName();

        int totalCount = service.getJobPostingCount(memberId);
        int totalPage = (int) Math.ceil((double) totalCount / SIZE_PER_PAGE_5);
        int pageNo = parsePageNo(currentShowPageNo, totalPage);

        int startRow = ((pageNo - 1) * SIZE_PER_PAGE_5) + 1;
        int endRow = startRow + SIZE_PER_PAGE_5 - 1;

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("memberId", memberId);
        paraMap.put("startRow", startRow);
        paraMap.put("endRow", endRow);

        List<JobPostingDTO> jobList = service.getJobPostingListPaing(paraMap);
        model.addAttribute("jobList", jobList);

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("sizePerPage", SIZE_PER_PAGE_5);
        model.addAttribute("currentShowPageNo", pageNo);
        model.addAttribute("totalPage", totalPage);

        // 선택된 공고 상세정보 조회(로그인한 기업 계정인지 추가 확인)
        if (jobId != null) {
            JobPostingDTO selectedJob = service.getJobPostingOne(jobId);

            if (selectedJob != null && memberId.equals(selectedJob.getMemberId())) {
                model.addAttribute("selectedJob", selectedJob);
            }
        }

        return VIEW_JOB_LIST;
    }

    // 채용공고 등록 페이지
    @GetMapping("/job/job_write")
    public String jobWriteForm(Model model) {
        model.addAttribute(ATTR_ACTIVE_MENU, MENU_JOB);

        List<EducationDTO> eduDtoList = service.selectEduList();
        model.addAttribute("eduDtoList", eduDtoList);

        List<JobCategoryDTO> cat1List = service.getRoots();
        model.addAttribute("cat1List", cat1List);

        model.addAttribute("skillCategoryList", service.getSkillCategoryWithSkills());
        model.addAttribute("region1List", service.getRegionLevel1());

        return VIEW_JOB_WRITE;
    }

    // 채용공고 수정 페이지 (job_write 템플릿 재사용 형태)
    // URL 직접 입력으로 페이지 접속 방지 추가
    @GetMapping("/job/update")
    public String jobUpdateForm(@RequestParam("jobId") long jobId,
                                Model model,
                                Authentication authentication) {

        model.addAttribute(ATTR_ACTIVE_MENU, MENU_JOB);

        if (authentication == null || authentication.getName() == null) {
            return REDIRECT_LOGIN;
        }

        String memberId = authentication.getName();
        JobPostingDTO post = service.getJobPostingOne(jobId);

        if (post == null || !memberId.equals(post.getMemberId())) {
            return REDIRECT_JOB_LIST;
        }

        // 신고 공고는 수정 페이지 진입 자체를 막음
        if (post.getIsHidden() != null && post.getIsHidden() == 1) {
            return REDIRECT_JOB_LIST;
        }

        model.addAttribute("jobId", jobId);
        model.addAttribute("eduDtoList", service.selectEduList());
        model.addAttribute("cat1List", service.getRoots());
        model.addAttribute("skillCategoryList", service.getSkillCategoryWithSkills());
        model.addAttribute("region1List", service.getRegionLevel1());

        return VIEW_JOB_UPDATE;
    }

    // 공고상세 팝업창 열기
    @GetMapping("/job/detail/{jobId}")
    public String jobDetailPopup(@PathVariable("jobId") Long jobId,
                                 Model model,
                                 Authentication authentication) {

        model.addAttribute(ATTR_ACTIVE_MENU, MENU_JOB);

        if (authentication == null || authentication.getName() == null) {
            return REDIRECT_LOGIN;
        }

        String memberId = authentication.getName();
        service.refreshJobPostingStatuses();

        JobPostingDTO post = service.getJobPostingOne(jobId);

        // 공고가 없거나 로그인한 기업의 공고가 아니면 목록으로 보낸다.
        if (post == null || !memberId.equals(post.getMemberId())) {
            return REDIRECT_JOB_LIST;
        }

        model.addAttribute("post", post);
        return VIEW_JOB_DETAIL_POPUP;
    }
    // ============================== 채용공고 ============================== //

    // ============================== 지원자 관리 ============================== //
    // 지원자 관리 리스트 페이지
    @GetMapping("/applicant/list")
    public String applicants(Model model, Authentication authentication) {
        model.addAttribute(ATTR_ACTIVE_MENU, MENU_APPLICANT);

        String memberId = authentication.getName();
        model.addAttribute("jobList", service.getJobPostingList(memberId));

        return VIEW_APPLICANT_LIST;
    }
    // ============================== 지원자 관리 ============================== //

    // ============================== 제안서 ============================== //
    // 제안서 관리 리스트 페이지
    @GetMapping("/offer/list")
    public String offers(Model model,
                         Authentication authentication,
                         @RequestParam(value = "targetMemberId", required = false) String targetMemberId,
                         @RequestParam(value = "targetMemberName", required = false) String targetMemberName,
                         @RequestParam(value = "targetResumeId", required = false) Long targetResumeId,
                         @RequestParam(value = "openOfferSend", defaultValue = "0") int openOfferSend) {

        model.addAttribute(ATTR_ACTIVE_MENU, MENU_OFFER);

        String memberId = authentication.getName();

        List<OfferListDTO> offerList = service.selectOfferList(memberId);
        model.addAttribute("offerList", offerList);

        List<JobPostingDTO> jobList = service.getJobPostingList(memberId);
        model.addAttribute("jobList", jobList);

        List<MemberSimpleDTO> receiverList = service.getReceiverList();
        model.addAttribute("receiverList", receiverList);

        model.addAttribute("scoutTargetMemberId", targetMemberId);
        model.addAttribute("scoutTargetMemberName", targetMemberName);
        model.addAttribute("scoutTargetResumeId", targetResumeId);
        model.addAttribute("openOfferSendFromTalent", openOfferSend == 1);

        return VIEW_OFFER_LIST;
    }
    // ============================== 제안서 ============================== //

    // ============================== 배너 ============================== //
    // 배너 목록 조회 페이지(페이징 처리)
    @GetMapping("/banner/list")
    public String ads(@RequestParam(value = "currentShowPageNo", required = false, defaultValue = "1") String currentShowPageNo,
                      Model model,
                      Authentication authentication,
                      HttpServletRequest request) {

        model.addAttribute(ATTR_ACTIVE_MENU, MENU_BANNER);

        service.refreshBannerStatuses();

        String memberId = authentication.getName();

        int totalCount = service.getBannerCountByMemberId(memberId);
        int totalPage = (int) Math.ceil((double) totalCount / SIZE_PER_PAGE_5);
        int pageNo = parsePageNo(currentShowPageNo, totalPage);

        int startRow = ((pageNo - 1) * SIZE_PER_PAGE_5) + 1;
        int endRow = startRow + SIZE_PER_PAGE_5 - 1;

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("memberId", memberId);
        paraMap.put("startRow", startRow);
        paraMap.put("endRow", endRow);

        List<BannerListDTO> bannerList = service.getBannerListByMemberIdPaging(paraMap);

        model.addAttribute("bannerList", bannerList);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("sizePerPage", SIZE_PER_PAGE_5);
        model.addAttribute("currentShowPageNo", pageNo);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("contextPath", request.getContextPath());

        return VIEW_BANNER_LIST;
    }

    // 배너 등록 페이지
    @GetMapping("/banner/write")
    public String bannerWriteForm(Model model, Authentication authentication) {
        model.addAttribute(ATTR_ACTIVE_MENU, MENU_BANNER);
        return VIEW_BANNER_WRITE;
    }
    // ============================== 배너 ============================== //

    // ============================== 포인트-지갑 ============================== //
    // 포인트 & 지갑 페이지
    @GetMapping("/wallet")
    public String wallet(@RequestParam(value = "menu", defaultValue = MENU_WALLET) String menu,
                         @RequestParam(value = "tab", defaultValue = MENU_WALLET) String tab,
                         @RequestParam(value = "currentShowPageNo", defaultValue = "1") String currentShowPageNoStr,
                         Authentication authentication,
                         Model model,
                         HttpServletRequest request) {

        model.addAttribute(ATTR_ACTIVE_MENU, menu);
        model.addAttribute(ATTR_ACTIVE_TAB, tab);

        if (authentication == null || authentication.getName() == null) {
            return REDIRECT_LOGIN;
        }

        String memberId = authentication.getName();
        model.addAttribute("impCode", impCode);

        int currentShowPageNo = parsePositivePageNo(currentShowPageNoStr);
        Map<String, Object> walletData = service.getWalletPageData(memberId, tab, currentShowPageNo, SIZE_PER_PAGE_5);
        model.addAllAttributes(walletData);

        int totalCount = (int) walletData.get("totalCount");
        String pageBar = buildWalletPageBar(tab, currentShowPageNo, SIZE_PER_PAGE_5, totalCount, request);

        model.addAttribute("pageBar", pageBar);

        return VIEW_WALLET;
    }

    // 결제_포인트용 페이징 처리를 위한 메서드
    private String buildWalletPageBar(String tab,
                                      int currentShowPageNo,
                                      int sizePerPage,
                                      int totalCount,
                                      HttpServletRequest request) {

        int totalPage = (int) Math.ceil((double) totalCount / sizePerPage);
        if (totalPage == 0) {
            return "";
        }

        String contextPath = request.getContextPath();
        String baseUrl = contextPath + "/company/wallet?menu=wallet&tab=" + tab + "&currentShowPageNo=";

        StringBuilder pageBar = new StringBuilder();
        pageBar.append("<div class='job-pagination-wrap'>");
        pageBar.append("<div class='job-pagination'>");

        // 이전
        if (currentShowPageNo > 1) {
            pageBar.append("<a class='job-page-btn' href='")
                   .append(baseUrl).append(currentShowPageNo - 1)
                   .append("'>이전</a>");
        } else {
            pageBar.append("<span class='job-page-btn is-disabled'>이전</span>");
        }

        // 번호
        // dead store였던 blockSize, pageNo는 실제 사용되지 않아 제거했다.
        for (int i = 1; i <= totalPage; i++) {
            if (i == currentShowPageNo) {
                pageBar.append("<span class='job-page-num active'>")
                       .append(i)
                       .append("</span>");
            } else {
                pageBar.append("<a class='job-page-num' href='")
                       .append(baseUrl).append(i)
                       .append("'>")
                       .append(i)
                       .append("</a>");
            }
        }

        // 다음
        if (currentShowPageNo < totalPage) {
            pageBar.append("<a class='job-page-btn' href='")
                   .append(baseUrl).append(currentShowPageNo + 1)
                   .append("'>다음</a>");
        } else {
            pageBar.append("<span class='job-page-btn is-disabled'>다음</span>");
        }

        pageBar.append("</div>");
        pageBar.append("</div>");

        return pageBar.toString();
    }
    // ============================== 포인트-지갑 ============================== //

    // ============================== 인재 검색 ============================== //
    private String buildTalentPageBar(TalentSearchConditionDTO searchDto, int totalCount, HttpServletRequest request) {
        int sizePerPage = (searchDto.getSize() == null || searchDto.getSize() < 1) ? TALENT_PAGE_SIZE : searchDto.getSize();
        int currentShowPageNo = (searchDto.getPage() == null || searchDto.getPage() < 1) ? 1 : searchDto.getPage();

        int totalPage = (int) Math.ceil((double) totalCount / sizePerPage);
        if (totalPage == 0) {
            return "";
        }

        int blockSize = 10;
        int pageNo = ((currentShowPageNo - 1) / blockSize) * blockSize + 1;
        int loop = 1;

        String baseUrl = request.getContextPath() + "/company/talent";
        String queryString = buildTalentQueryString(request);

        StringBuilder pageBar = new StringBuilder();
        pageBar.append("<div class='talent-pagebar-wrap'>");
        pageBar.append("<ul class='talent-pagebar'>");

        // 이전 블록
        if (pageNo != 1) {
            pageBar.append("<li>")
                   .append("<a href='").append(baseUrl).append("?menu=talent&page=").append(pageNo - 1).append(queryString).append("'>")
                   .append("&laquo;")
                   .append("</a>")
                   .append("</li>");
        }

        while (!(loop > blockSize || pageNo > totalPage)) {
            if (pageNo == currentShowPageNo) {
                pageBar.append("<li>")
                       .append("<span class='is-current'>").append(pageNo).append("</span>")
                       .append("</li>");
            } else {
                pageBar.append("<li>")
                       .append("<a href='").append(baseUrl).append("?menu=talent&page=").append(pageNo).append(queryString).append("'>")
                       .append(pageNo)
                       .append("</a>")
                       .append("</li>");
            }

            loop++;
            pageNo++;
        }

        // 다음 블록
        if (pageNo <= totalPage) {
            pageBar.append("<li>")
                   .append("<a href='").append(baseUrl).append("?menu=talent&page=").append(pageNo).append(queryString).append("'>")
                   .append("&raquo;")
                   .append("</a>")
                   .append("</li>");
        }

        pageBar.append("</ul>");
        pageBar.append("</div>");

        return pageBar.toString();
    }

    // 인재검색용 페이징 처리를 위한 메서드
    private String buildTalentQueryString(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();

        Map<String, String[]> paraMap = request.getParameterMap();

        paraMap.forEach((key, values) -> {
            // page와 menu는 pageBar에서 새로 붙이므로 제외한다.
            if ("page".equals(key) || "menu".equals(key)) {
                return;
            }

            if (values == null) {
                return;
            }

            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }

                try {
                    sb.append("&")
                      .append(java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8))
                      .append("=")
                      .append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {
                    // 인코딩 실패 시 해당 파라미터만 무시한다.
                }
            }
        });

        return sb.toString();
    }

    // 인재검색 페이지
    @GetMapping("/talent")
    public String talent(@RequestParam(value = "menu", defaultValue = "talent") String menu,
                         @ModelAttribute TalentSearchConditionDTO searchDto,
                         Model model,
                         HttpServletRequest request) {

        model.addAttribute(ATTR_ACTIVE_MENU, menu);

        if (searchDto.getPage() == null || searchDto.getPage() < 1) {
            searchDto.setPage(1);
        }
        searchDto.setSize(TALENT_PAGE_SIZE);

        model.addAttribute("jobCategoryList", service.getJobCategoryList());
        model.addAttribute("skillCategoryList", service.getSkillCategoryList());
        model.addAttribute("skillList", service.getSkillList());

        int totalCount = service.getPublicPrimaryResumeCount(searchDto);
        int totalPage = (int) Math.ceil((double) totalCount / searchDto.getSize());

        if (totalPage > 0 && searchDto.getPage() > totalPage) {
            searchDto.setPage(totalPage);
        }

        model.addAttribute("resumeList", service.getPublicPrimaryResumeList(searchDto));
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("searchDto", searchDto);
        model.addAttribute("pageBar", buildTalentPageBar(searchDto, totalCount, request));

        return VIEW_TALENT_SEARCH;
    }

    // 공개 이력서 상세
    @GetMapping("/talent/detail")
    public String talentDetail(@RequestParam("resumeId") Long resumeId,
                               Model model,
                               Authentication authentication) {

        model.addAttribute(ATTR_ACTIVE_MENU, MENU_TALENT);

        TalentResumeDetailDTO dto = service.getPublicPrimaryResumeDetail(resumeId);

        if (dto == null) {
            return REDIRECT_TALENT;
        }

        String memberId = authentication.getName();
        List<OfferListDTO> offerList = service.selectOfferList(memberId);

        model.addAttribute("resume", dto);
        model.addAttribute("offerList", offerList);

        return VIEW_TALENT_DETAIL;
    }
    // ============================== 인재 검색 ============================== //

    /**
     * 페이지 번호 문자열을 정수로 변환하고 범위를 보정한다.
     * 여러 곳에서 반복되는 페이지 파싱 로직을 공통화하기 위한 메서드다.
     */
    private int parsePageNo(String pageNoStr, int totalPage) {
        int pageNo = 1;

        try {
            pageNo = Integer.parseInt(pageNoStr);
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

    /**
     * 1 이상인 페이지 번호만 허용하는 단순 파싱 메서드다.
     * wallet 쪽처럼 totalPage 보정이 필요 없는 경우에 사용한다.
     */
    private int parsePositivePageNo(String pageNoStr) {
        int pageNo = 1;

        try {
            pageNo = Integer.parseInt(pageNoStr);
        } catch (NumberFormatException e) {
            pageNo = 1;
        }

        return Math.max(pageNo, 1);
    }
}