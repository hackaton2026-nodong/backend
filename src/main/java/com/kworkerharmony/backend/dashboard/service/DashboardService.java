package com.kworkerharmony.backend.dashboard.service;

import com.kworkerharmony.backend.alert.domain.AlertRepository;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.checklist.domain.CaseChecklistStatusRepository;
import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.dashboard.domain.dto.response.EmployerDashboardResponse;
import com.kworkerharmony.backend.dashboard.domain.dto.response.WorkerDashboardResponse;
import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.DocumentRepository;
import com.kworkerharmony.backend.document.DocumentStatus;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.reference.checklist.ChecklistCatalog;
import com.kworkerharmony.backend.reference.educationvenue.EducationVenueCatalog;
import com.kworkerharmony.backend.reference.organization.OrganizationCatalog;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CaseRepository caseRepository;
    private final CaseChecklistStatusRepository caseChecklistStatusRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ChecklistCatalog checklistCatalog;
    private final EducationVenueCatalog educationVenueCatalog;
    private final OrganizationCatalog organizationCatalog;

    @Transactional(readOnly = true)
    public WorkerDashboardResponse getWorkerDashboard(UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        validateUserType(user, UserType.WORKER, "Worker dashboard is only available for worker accounts");
        Case caseEntity = resolveWorkerDashboardCase(user.getId()).orElse(null);
        List<Document> documents = caseEntity == null
                ? List.of()
                : documentRepository.findAllByCaseIdOrderByCreatedAtDesc(caseEntity.getId());
        long totalChecklistItems = checklistCatalog.getItems().size();
        long completedChecklistCount = caseEntity == null
                ? 0L
                : caseChecklistStatusRepository.findByCaseEntityId(caseEntity.getId()).stream()
                        .filter(status -> status.getStatus() == ChecklistStatus.COMPLETED)
                        .count();
        long reviewRequiredChecklistCount = caseEntity == null
                ? 0L
                : caseChecklistStatusRepository.findByCaseEntityId(caseEntity.getId()).stream()
                        .filter(status -> status.getStatus() == ChecklistStatus.REVIEW_REQUIRED)
                        .count();
        long failedDocumentCount = documents.stream()
                .filter(document -> document.getStatus() == DocumentStatus.FAILED)
                .count();
        long analyzedDocumentCount = documents.stream()
                .filter(document -> isCountedAsAnalyzed(document.getStatus()))
                .count();
        long unreadAlertCount = alertRepository.countByUserIdAndIsReadFalse(user.getId());
        LocalDate nearestExpiryDate = documents.stream()
                .map(Document::getExpiresAt)
                .filter(date -> date != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        long unresolvedChecklistCount = Math.max(totalChecklistItems - completedChecklistCount, 0L);

        return new WorkerDashboardResponse(
                new WorkerDashboardResponse.Header(
                        user.getId(),
                        user.getName(),
                        user.getUserType(),
                        LocalDate.now(),
                        caseEntity == null ? null : caseEntity.getId()
                ),
                buildAgentCenter(caseEntity, documents, reviewRequiredChecklistCount, unreadAlertCount),
                buildSummaryCards(reviewRequiredChecklistCount + failedDocumentCount, totalChecklistItems, completedChecklistCount, analyzedDocumentCount, nearestExpiryDate),
                buildTodayActions(caseEntity, documents, reviewRequiredChecklistCount, unreadAlertCount, unresolvedChecklistCount),
                buildRecommendationSlot(caseEntity, user),
                buildNoticePanel(caseEntity, nearestExpiryDate, failedDocumentCount, reviewRequiredChecklistCount)
        );
    }

    @Transactional(readOnly = true)
    public EmployerDashboardResponse getEmployerDashboard(UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        validateUserType(user, UserType.EMPLOYER, "Employer dashboard is only available for employer accounts");

        return new EmployerDashboardResponse(
                user.getId(),
                user.getUserType(),
                caseRepository.countByEmployerIdAndStatus(user.getId(), CaseStatus.ACTIVE),
                caseChecklistStatusRepository.countByCaseEntityEmployerId(user.getId()),
                caseChecklistStatusRepository.countByCaseEntityEmployerIdAndStatus(user.getId(), ChecklistStatus.COMPLETED),
                alertRepository.countByUserIdAndIsReadFalse(user.getId())
        );
    }

    private Optional<Case> resolveWorkerDashboardCase(Long userId) {
        return caseRepository.findFirstByWorkerIdAndStatusOrderByCreatedAtDesc(userId, CaseStatus.ACTIVE)
                .or(() -> caseRepository.findFirstByWorkerIdAndStatusOrderByCreatedAtDesc(userId, CaseStatus.PENDING));
    }

    private User getUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private void validateUserType(User user, UserType expectedType, String message) {
        if (user.getUserType() != expectedType) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, message);
        }
    }

    private WorkerDashboardResponse.AgentCenter buildAgentCenter(
            Case caseEntity,
            List<Document> documents,
            long reviewRequiredChecklistCount,
            long unreadAlertCount
    ) {
        if (caseEntity == null) {
            return new WorkerDashboardResponse.AgentCenter(
                    "아직 연결된 케이스가 없어요",
                    "회사 연결 또는 케이스 생성이 완료되면 문서 분석과 체크리스트 흐름이 시작됩니다.",
                    List.of(new WorkerDashboardResponse.ActionLink("알림 보기", "/alerts")),
                    List.of("CASE")
            );
        }
        if (reviewRequiredChecklistCount > 0) {
            return new WorkerDashboardResponse.AgentCenter(
                    "체크리스트 검토가 필요해요",
                    "문서 분석 결과와 연결된 점검 항목에 검토가 필요한 항목이 있습니다.",
                    List.of(
                            new WorkerDashboardResponse.ActionLink("체크리스트 이동", "/checklists?caseId=" + caseEntity.getId()),
                            new WorkerDashboardResponse.ActionLink("문서 분석 보기", "/cases/" + caseEntity.getId() + "/documents")
                    ),
                    List.of("CHECKLIST", "DOCUMENT")
            );
        }
        if (documents.isEmpty()) {
            return new WorkerDashboardResponse.AgentCenter(
                    "문서를 먼저 업로드해 주세요",
                    "근로계약서나 급여 관련 문서를 올리면 상태 기반 안내가 시작됩니다.",
                    List.of(new WorkerDashboardResponse.ActionLink("문서 분석 보기", "/cases/" + caseEntity.getId() + "/documents")),
                    List.of("DOCUMENT")
            );
        }
        if (unreadAlertCount > 0) {
            return new WorkerDashboardResponse.AgentCenter(
                    "확인하지 않은 알림이 있어요",
                    "최근 상태 변화와 관련된 조치 알림을 먼저 확인해 주세요.",
                    List.of(new WorkerDashboardResponse.ActionLink("알림 보기", "/alerts")),
                    List.of("ALERT")
            );
        }
        return new WorkerDashboardResponse.AgentCenter(
                "현재 상태는 안정적이에요",
                "문서와 체크리스트 흐름을 기준으로 다음 행동을 정리해 두었습니다.",
                List.of(
                        new WorkerDashboardResponse.ActionLink("AI 상담 이동", "/consultations?caseId=" + caseEntity.getId()),
                        new WorkerDashboardResponse.ActionLink("체크리스트 이동", "/checklists?caseId=" + caseEntity.getId())
                ),
                List.of("CASE", "DOCUMENT")
        );
    }

    private List<WorkerDashboardResponse.SummaryCard> buildSummaryCards(
            long riskCount,
            long totalChecklistItems,
            long completedChecklistCount,
            long analyzedDocumentCount,
            LocalDate nearestExpiryDate
    ) {
        String dDayValue = "-";
        String dDaySubtitle = "등록된 일정 없음";
        String dDaySeverity = "neutral";
        if (nearestExpiryDate != null) {
            long dDay = ChronoUnit.DAYS.between(LocalDate.now(), nearestExpiryDate);
            dDayValue = dDay >= 0 ? "D-" + dDay : "D+" + Math.abs(dDay);
            dDaySubtitle = nearestExpiryDate + " 기준";
            dDaySeverity = dDay <= 7 ? "high" : "medium";
        }

        String checklistValue = totalChecklistItems == 0
                ? "0%"
                : Math.round((completedChecklistCount * 100.0) / totalChecklistItems) + "%";

        return List.of(
                new WorkerDashboardResponse.SummaryCard("risks", "미처리 위험 항목", String.valueOf(riskCount), "즉시 확인 필요", riskCount > 0 ? "high" : "neutral"),
                new WorkerDashboardResponse.SummaryCard("checklistProgress", "체크리스트 진행률", checklistValue, completedChecklistCount + " / " + totalChecklistItems + " 항목 완료", "medium"),
                new WorkerDashboardResponse.SummaryCard("analyzedDocuments", "분석된 문서 수", String.valueOf(analyzedDocumentCount), "최소 저장 성공 기준", "low"),
                new WorkerDashboardResponse.SummaryCard("nextSchedule", "다가오는 일정", dDayValue, dDaySubtitle, dDaySeverity)
        );
    }

    private List<WorkerDashboardResponse.TodayAction> buildTodayActions(
            Case caseEntity,
            List<Document> documents,
            long reviewRequiredChecklistCount,
            long unreadAlertCount,
            long unresolvedChecklistCount
    ) {
        if (caseEntity == null) {
            return List.of(
                    new WorkerDashboardResponse.TodayAction(
                            "CASE",
                            "연결된 케이스 확인",
                            "회사 연결 또는 케이스 배정 여부를 먼저 확인해 주세요.",
                            "알림 보기",
                            "/alerts",
                            "HIGH"
                    )
            );
        }

        java.util.ArrayList<WorkerDashboardResponse.TodayAction> actions = new java.util.ArrayList<>();
        if (!documents.isEmpty()) {
            actions.add(new WorkerDashboardResponse.TodayAction(
                    "DOCUMENT",
                    "문서 분석 결과 검토",
                    "최근 업로드된 문서의 분석 결과와 근거를 확인해 주세요.",
                    "바로가기",
                    "/cases/" + caseEntity.getId() + "/documents",
                    "HIGH"
            ));
        }
        if (reviewRequiredChecklistCount > 0 || unresolvedChecklistCount > 0) {
            actions.add(new WorkerDashboardResponse.TodayAction(
                    "CHECKLIST",
                    "체크리스트 점검",
                    "공식 체크리스트의 미완료 또는 검토 필요 항목을 확인해 주세요.",
                    "문항 보기",
                    "/checklists?caseId=" + caseEntity.getId(),
                    reviewRequiredChecklistCount > 0 ? "HIGH" : "MEDIUM"
            ));
        }
        actions.add(new WorkerDashboardResponse.TodayAction(
                "CONSULTATION",
                "AI 상담으로 문서 설명 받기",
                "이해하기 어려운 항목은 현재 케이스 문맥으로 상담할 수 있습니다.",
                "상담하기",
                "/consultations?caseId=" + caseEntity.getId(),
                "MEDIUM"
        ));
        if (unreadAlertCount > 0) {
            actions.add(new WorkerDashboardResponse.TodayAction(
                    "ALERT",
                    "최근 알림 확인",
                    "새로 생성된 안내와 조치 알림을 확인해 주세요.",
                    "알림 보기",
                    "/alerts",
                    "LOW"
            ));
        }
        return actions.stream().limit(3).toList();
    }

    private WorkerDashboardResponse.RecommendationSlot buildRecommendationSlot(Case caseEntity, User user) {
        String region = caseEntity == null ? null : caseEntity.getRegion();
        List<String> reasonTags = caseEntity == null
                ? List.of("language:" + user.getLanguageCode())
                : List.of(
                        "region:" + region,
                        "language:" + user.getLanguageCode(),
                        "industry:" + caseEntity.getIndustry()
                );
        List<WorkerDashboardResponse.RecommendationItem> items = educationVenueCatalog.findByRegion(region, 3)
                .stream()
                .map(venue -> {
                    String orgName = organizationCatalog.findByOrgCd(venue.orgCd())
                            .map(org -> org.nameKo())
                            .orElse("");
                    return new WorkerDashboardResponse.RecommendationItem(
                            "EDUCATION",
                            venue.name(),
                            orgName + " | " + venue.address(),
                            "/education/venues/" + venue.eduOrgCd()
                    );
                })
                .toList();
        return new WorkerDashboardResponse.RecommendationSlot(
                "추천 교육장",
                "근무 지역 기준으로 가까운 교육장을 안내합니다.",
                reasonTags,
                items
        );
    }

    private WorkerDashboardResponse.NoticePanel buildNoticePanel(
            Case caseEntity,
            LocalDate nearestExpiryDate,
            long failedDocumentCount,
            long reviewRequiredChecklistCount
    ) {
        if (caseEntity == null) {
            return new WorkerDashboardResponse.NoticePanel(
                    "안내",
                    "아직 연결된 케이스가 없어 개인 상태판만 표시하고 있습니다.",
                    "info",
                    "알림 보기",
                    "/alerts"
            );
        }
        if (nearestExpiryDate != null && ChronoUnit.DAYS.between(LocalDate.now(), nearestExpiryDate) <= 30) {
            return new WorkerDashboardResponse.NoticePanel(
                    "주의 사항",
                    "만료 일정이 30일 이내로 다가온 문서가 있습니다. 관련 문서를 먼저 확인해 주세요.",
                    "high",
                    "관련 문서 보기",
                    "/cases/" + caseEntity.getId() + "/documents"
            );
        }
        if (failedDocumentCount > 0) {
            return new WorkerDashboardResponse.NoticePanel(
                    "문서 처리 실패",
                    "다시 업로드하거나 분석이 필요한 문서가 있습니다.",
                    "high",
                    "문서 보기",
                    "/cases/" + caseEntity.getId() + "/documents"
            );
        }
        if (reviewRequiredChecklistCount > 0) {
            return new WorkerDashboardResponse.NoticePanel(
                    "체크리스트 검토 필요",
                    "문서와 연결된 공식 점검 항목 중 확인이 필요한 문항이 있습니다.",
                    "medium",
                    "체크리스트 이동",
                    "/checklists?caseId=" + caseEntity.getId()
            );
        }
        return new WorkerDashboardResponse.NoticePanel(
                "상태 안정",
                "현재 확인된 위험 항목이 없습니다. 다음 행동 카드 기준으로 진행해 주세요.",
                "low",
                "AI 상담 이동",
                "/consultations?caseId=" + caseEntity.getId()
        );
    }

    private boolean isCountedAsAnalyzed(DocumentStatus status) {
        return status == DocumentStatus.HASHED
                || status == DocumentStatus.ANCHORED_ON_CHAIN
                || status == DocumentStatus.OCR_PROCESSING
                || status == DocumentStatus.OCR_COMPLETED
                || status == DocumentStatus.STRUCTURED
                || status == DocumentStatus.ANALYZED;
    }
}
