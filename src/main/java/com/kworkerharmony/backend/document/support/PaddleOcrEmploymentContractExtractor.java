package com.kworkerharmony.backend.document.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PaddleOcrEmploymentContractExtractor {

    public static final String SCHEMA_VERSION = "employment-contract-v1";
    public static final String SOURCE_ENGINE = "PADDLE_OCR";

    private static final Pattern CONTRACT_PERIOD = Pattern.compile("(\\d{2})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일\\s*~\\s*(\\d{2})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern CONTRACT_PERIOD_EN = Pattern.compile("from\\((\\d{2})/(\\d{1,2})/(\\d{1,2})\\s*YY/MM/DD\\)\\s*to\\((\\d{2})/(\\d{1,2})/(\\d{1,2})\\s*YY/MM/DD\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORKING_HOURS_RANGE = Pattern.compile("(\\d{1,2})\\s*(?::|시|時|人)\\s*(\\d{1,2})\\s*(?:분|是)?\\s*\\)?\\s*(?:~|to\\s*\\(?|-)\\s*\\(?\\s*(\\d{1,2})\\s*(?::|시|時|人)\\s*(\\d{1,2})\\s*(?:분|是)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern OVERTIME = Pattern.compile("1일 평균 시간외 근로시간:\\s*(\\d+)\\s*시간|average daily over time:\\s*(\\d+)\\s*hours", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARIABLE_HOURS = Pattern.compile("변동 가능:\\s*(\\d+)\\s*시간 이내|up to\\s*(\\d+)\\s*hours", Pattern.CASE_INSENSITIVE);
    private static final Pattern BREAK_TIME = Pattern.compile("휴게시간\\s*1\\s*일\\s*(\\d+)\\s*분");
    private static final Pattern BREAK_TIME_EN = Pattern.compile("\\(\\s*(\\d+)\\s*\\)\\s*minutes per day", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTHLY_WAGE = Pattern.compile("월\\s*통상임금\\s*\\(\\s*([0-9,]+)\\s*\\)\\s*원|monthly normal wages\\s*\\(\\s*([0-9,]+)\\s*\\)\\s*won", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASE_PAY = Pattern.compile("기본급\\s*\\[\\s*월급\\s*]\\s*\\(\\s*([0-9,]+)\\s*\\)\\s*원|basic pay\\s*\\[?\\s*\\(?\\s*monthly\\s*\\)?\\s*wage\\s*]?\\s*\\(\\s*([0-9,]+)\\s*\\)\\s*won", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRODUCTION_ALLOWANCE = Pattern.compile("생산\\s*수당\\s*:\\s*([0-9,oO]+)\\s*(?:\\)?\\s*원|\\)\\s*won)|production benefits\\s*:\\s*([0-9,oO]+)\\s*\\)?\\s*won", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEAL_ALLOWANCE = Pattern.compile("식대\\s*수당\\s*:\\s*([0-9,oO]+)\\s*(?:\\)?\\s*원|\\)\\s*won)|meal benefits\\s*:\\s*([0-9,oO]+)\\s*\\)?\\s*won", Pattern.CASE_INSENSITIVE);
    private static final Pattern BONUS = Pattern.compile("상여금\\s*\\(?\\s*([0-9,oO]+)\\s*원?\\)?|bonus\\s*:\\s*\\(\\s*([0-9,oO]+)\\s*\\)\\s*won", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAYMENT_DAY = Pattern.compile("임금지급일\\s*매월\\s*\\(\\s*(\\d{1,2})\\s*\\)\\s*일|every\\s*\\(\\s*(\\d{1,2})\\s*\\)\\s*th", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAYMENT_DAY_MONTH_END = Pattern.compile("매월\\s*\\(\\s*말\\s*\\)\\s*일|every\\s*\\(\\s*last\\s*\\)\\s*day", Pattern.CASE_INSENSITIVE);
    private static final Pattern DORMITORY_DEDUCTION = Pattern.compile("숙박시설\\s*제공\\s*시\\s*근로자\\s*부담금액\\s*:\\s*매월\\s*([0-9,oO]+)\\s*원|cost of accommodation paid by employee\\s*:\\s*([0-9,oO]+)\\s*won", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEAL_DEDUCTION = Pattern.compile("식사\\s*제공\\s*시\\s*근로자\\s*부담금액\\s*:\\s*매월\\s*([0-9,oO]+)\\s*\\)?원|cost of meals paid by employee\\s*:\\s*([0-9,oO]+)\\s*won", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIGNED_DATE = Pattern.compile("(20\\d{2})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2})\\.");
    private static final Pattern PROBATION_MONTHS = Pattern.compile("specify other\\s*:\\s*(\\d+)\\s*month|\\[(\\d+)]\\s*개월", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public PaddleOcrEmploymentContractExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EmploymentContractExtractionPayload extract(JsonNode ocrResult) {
        OcrText ocrText = collectText(ocrResult);
        String text = ocrText.text();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        ObjectNode contractTerms = root.putObject("contractTerms");
        ArrayNode evidenceRefs = root.putArray("evidenceRefs");
        ArrayNode checklistCodes = root.putArray("candidateChecklistItemCodes");

        addDocumentType(contractTerms, text);
        addContractPeriod(contractTerms, evidenceRefs, ocrText, text);
        addProbation(contractTerms, evidenceRefs, ocrText, text);
        addWork(contractTerms, evidenceRefs, ocrText, text);
        addWorkingHours(contractTerms, evidenceRefs, ocrText, text);
        addBreakTime(contractTerms, evidenceRefs, ocrText, text);
        addHolidays(contractTerms, evidenceRefs, ocrText, text);
        addWage(contractTerms, evidenceRefs, ocrText, text);
        addDormitoryAndMeals(contractTerms, evidenceRefs, ocrText, text);
        addSignature(contractTerms, evidenceRefs, ocrText, text);
        addChecklistCodes(checklistCodes);

        String reviewRequiredReason = reviewRequiredReason(contractTerms);
        try {
            String payloadJson = objectMapper.writeValueAsString(root);
            return new EmploymentContractExtractionPayload(
                    payloadJson,
                    DocumentCrypto.sha256Hex(payloadJson),
                    reviewRequiredReason
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize extraction payload", ex);
        }
    }

    private OcrText collectText(JsonNode root) {
        StringBuilder builder = new StringBuilder();
        List<TextBlockEvidence> evidence = new ArrayList<>();
        appendMarkdownText(root.path("layoutParsingResults"), builder);
        appendBlockContent(root.path("layoutParsingResults"), builder, evidence);
        return new OcrText(builder.toString()
                .replace("&nbsp;", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim(), evidence);
    }

    private void appendMarkdownText(JsonNode results, StringBuilder builder) {
        if (!results.isArray()) {
            return;
        }
        results.forEach(result -> append(builder, result.path("markdown").path("text").asText("")));
    }

    private void appendBlockContent(JsonNode results, StringBuilder builder, List<TextBlockEvidence> evidence) {
        if (!results.isArray()) {
            return;
        }
        for (JsonNode result : results) {
            JsonNode blocks = result.path("prunedResult").path("parsing_res_list");
            if (!blocks.isArray()) {
                continue;
            }
            blocks.forEach(block -> {
                String content = block.path("block_content").asText("");
                append(builder, content);
                if (!content.isBlank()) {
                    evidence.add(new TextBlockEvidence(
                            content,
                            block.path("page").asInt(result.path("page").asInt(1)),
                            block.path("block_bbox"),
                            block.path("confidence").isNumber() ? block.path("confidence").asDouble() : 0.8
                    ));
                }
            });
        }
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append('\n').append(value);
        }
    }

    private void addDocumentType(ObjectNode contractTerms, String text) {
        ObjectNode document = contractTerms.putObject("document");
        document.put("documentForm", contains(text, "표준근로계약서") ? "STANDARD_LABOR_CONTRACT" : "UNKNOWN");
        document.put("standardContractUsed", contains(text, "표준근로계약서"));
    }

    private void addContractPeriod(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode period = contractTerms.putObject("contractPeriod");
        Matcher matcher = CONTRACT_PERIOD.matcher(text);
        if (matcher.find()) {
            period.put("status", "FOUND");
            period.put("contractStartDate", date(matcher.group(1), matcher.group(2), matcher.group(3)));
            period.put("contractEndDate", date(matcher.group(4), matcher.group(5), matcher.group(6)));
            evidence(evidenceRefs, ocrText, "contractPeriod", "계약기간 [DATE] ~ [DATE]", matcher.group());
            return;
        }
        matcher = CONTRACT_PERIOD_EN.matcher(text);
        if (matcher.find()) {
            period.put("status", "FOUND");
            period.put("contractStartDate", date(matcher.group(1), matcher.group(2), matcher.group(3)));
            period.put("contractEndDate", date(matcher.group(4), matcher.group(5), matcher.group(6)));
            evidence(evidenceRefs, ocrText, "contractPeriod", "from [DATE] to [DATE]", matcher.group());
            return;
        }
        period.put("status", "MISSING");
    }

    private void addProbation(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode probation = contractTerms.putObject("probation");
        boolean included = text.contains("수습기간") && (checkedNear(text, "활용") || checkedNear(text, "Included"));
        probation.put("status", included ? "FOUND" : "MISSING");
        probation.put("included", included);
        if (included) {
            Integer months = findInteger(PROBATION_MONTHS, text);
            probation.put("months", months == null ? 1 : months);
            evidence(evidenceRefs, ocrText, "probation", "수습기간 [CHECKED] [MONTHS]개월", "수습기간");
        }
    }

    private void addWork(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode work = contractTerms.putObject("work");
        work.put("industryCategory", contains(text, "제조업") || containsLower(text, "manufacturing") ? "MANUFACTURING" : "UNKNOWN");
        work.put("workplaceRegion", contains(text, "안산") || containsLower(text, "ansan") ? "GYEONGGI_ANSAN" : "UNKNOWN");
        if (contains(text, "자동차 금속부품")) {
            work.put("businessCategory", "AUTOMOTIVE_METAL_PARTS");
        } else if (contains(text, "공장업무 전반") || containsLower(text, "overall factory work")) {
            work.put("businessCategory", "OVERALL_FACTORY_WORK");
        } else {
            work.put("businessCategory", "UNKNOWN");
        }
        String jobEvidenceNeedle = null;
        String jobMaskedExcerpt = null;
        if (contains(text, "금속부품 조립")) {
            work.put("jobCategory", "METAL_PARTS_ASSEMBLY_INSPECTION_PACKAGING");
            jobEvidenceNeedle = "금속부품 조립";
            jobMaskedExcerpt = "직무내용: 금속부품 조립, 품질검사, 포장작업";
        } else if (contains(text, "기타 사용자 지시업무") || containsLower(text, "other user")) {
            work.put("jobCategory", "OTHER_USER_INSTRUCTED_TASKS");
            jobEvidenceNeedle = contains(text, "기타 사용자 지시업무") ? "기타 사용자 지시업무" : "other user";
            jobMaskedExcerpt = "직무내용: 기타 사용자 지시업무";
        } else {
            work.put("jobCategory", "UNKNOWN");
        }
        if (jobEvidenceNeedle != null) {
            evidence(evidenceRefs, ocrText, "work.jobCategory", jobMaskedExcerpt, jobEvidenceNeedle);
        }
    }

    private void addWorkingHours(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode workingHours = contractTerms.putObject("workingHours");
        Matcher matcher = WORKING_HOURS_RANGE.matcher(text);
        if (matcher.find()) {
            String startHour = matcher.group(1);
            String startMinute = matcher.group(2);
            String endHour = matcher.group(3);
            String endMinute = matcher.group(4);
            workingHours.put("status", "FOUND");
            workingHours.put("startTime", twoDigits(startHour) + ":" + twoDigits(startMinute));
            workingHours.put("endTime", twoDigits(endHour) + ":" + twoDigits(endMinute));
            evidence(evidenceRefs, ocrText, "workingHours", "[TIME] ~ [TIME]",
                    timeEvidenceCandidates(matcher.group(), startHour, startMinute, endHour, endMinute));
        } else {
            workingHours.put("status", "MISSING");
        }
        putInteger(workingHours, "overtimeHoursPerDay", OVERTIME, text);
        putInteger(workingHours, "maxVariableHoursPerDay", VARIABLE_HOURS, text);
        workingHours.put("shiftSystem", false);
    }

    private void addBreakTime(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode breakTime = contractTerms.putObject("breakTime");
        Integer minutes = findInteger(BREAK_TIME, text);
        if (minutes == null) {
            minutes = findInteger(BREAK_TIME_EN, text);
        }
        if (minutes == null) {
            breakTime.put("status", "MISSING");
            return;
        }
        breakTime.put("status", "FOUND");
        breakTime.put("minutesPerDay", minutes);
        evidence(evidenceRefs, ocrText, "breakTime.minutesPerDay", "휴게시간 [MINUTES]분", "휴게시간");
    }

    private void addHolidays(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode holidays = contractTerms.putObject("holidays");
        boolean found = contains(text, "6. 휴일") || contains(text, "6.휴일") || containsLower(text, "holidays");
        holidays.put("status", found ? "FOUND" : "MISSING");
        holidays.put("sunday", checkedOption(text, "일요일") || checkedOption(text, "Sunday"));
        holidays.put("legalHoliday", checkedOption(text, "공휴일") || checkedOption(text, "Legal holiday"));
        holidays.put("legalHolidayPaid", checkedOption(text, "유급") || checkedOption(text, "Paid"));
        holidays.put("everySaturday", checkedOption(text, "매주 토요일") || checkedOption(text, "Every Saturday"));
        boolean otherHoliday = checkedOption(text, "기타") || checkedOption(text, "etc.");
        holidays.put("otherHoliday", otherHoliday);
        if (otherHoliday && (contains(text, "격주 일요일") || containsLower(text, "sunday off twice a month"))) {
            holidays.put("otherHolidayDescription", "SUNDAY_TWICE_A_MONTH");
        }
        if (found) {
            evidence(evidenceRefs, ocrText, "holidays", "휴일 [CHECKED]일요일 [CHECKED]공휴일 [CHECKED]매주 토요일", "휴일");
        }
    }

    private void addWage(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode wage = contractTerms.putObject("wage");
        Integer monthlyWage = findAmount(MONTHLY_WAGE, text);
        if (monthlyWage == null) {
            wage.put("status", "MISSING");
        } else {
            wage.put("status", "FOUND");
            wage.put("amount", monthlyWage);
            wage.put("currency", "KRW");
            wage.put("period", "MONTHLY");
            evidence(evidenceRefs, ocrText, "wage.amount", "월 통상임금 ([AMOUNT])원", "월 통상임금");
        }
        putAmount(wage, "basePay", BASE_PAY, text);
        putAmount(wage, "bonusAmount", BONUS, text);
        putInteger(wage, "paymentDay", PAYMENT_DAY, text);
        if (!wage.has("paymentDay") && PAYMENT_DAY_MONTH_END.matcher(text).find()) {
            wage.put("paymentDayType", "MONTH_END");
        }
        wage.put("paymentMethod", paymentMethod(text));
        wage.put("overtimeNightHolidayPremiumMentioned", contains(text, "50%를 가산") || containsLower(text, "50% more"));

        ArrayNode fixedAllowances = wage.putArray("fixedAllowances");
        addAllowance(fixedAllowances, "PRODUCTION", PRODUCTION_ALLOWANCE, text);
        addAllowance(fixedAllowances, "MEAL", MEAL_ALLOWANCE, text);
    }

    private void addDormitoryAndMeals(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode dormitory = contractTerms.putObject("dormitory");
        boolean dormitoryNotProvided = checkedAfter(text, "숙박시설 제공 여부", "미제공", 120)
                || checkedAfter(text, "Provision of accommodation", "Not provided", 160);
        boolean dormitoryProvided = (checkedAfter(text, "숙박시설 제공 여부", "제공", 120)
                || checkedAfter(text, "Provision of accommodation", "Provided", 160))
                && !dormitoryNotProvided;
        dormitory.put("status", contains(text, "숙박시설 제공") ? "FOUND" : "MISSING");
        dormitory.put("provided", dormitoryProvided);
        if (checkedOption(text, "컨테이너") || checkedOption(text, "Container boxes")) {
            dormitory.put("typeCategory", "CONTAINER_BOX");
        } else {
            dormitory.put("typeCategory", contains(text, "기숙사") ? "DORMITORY" : "UNKNOWN");
        }
        putAmount(dormitory, "deductionAmount", DORMITORY_DEDUCTION, text);
        if (dormitoryProvided) {
            evidence(evidenceRefs, ocrText, "dormitory.provided", "숙박시설 제공 여부: [CHECKED]제공", "숙박시설 제공");
        }

        ObjectNode meals = contractTerms.putObject("meals");
        boolean mealNotProvided = checkedAfter(text, "식사 제공 여부", "미제공", 160)
                || checkedAfter(text, "Provision of meals", "Not provided", 200);
        boolean breakfastProvided = checkedOption(text, "조식") || checkedOption(text, "breakfast");
        boolean lunchProvided = checkedOption(text, "중식") || checkedOption(text, "lunch");
        boolean dinnerProvided = checkedOption(text, "석식") || checkedOption(text, "dinner");
        boolean mealProvided = (breakfastProvided || lunchProvided || dinnerProvided
                || checkedAfter(text, "Provision of meals", "Provided", 120))
                && !mealNotProvided;
        meals.put("status", contains(text, "식사 제공") ? "FOUND" : "MISSING");
        meals.put("provided", mealProvided);
        meals.put("notProvided", mealNotProvided);
        ArrayNode providedMeals = meals.putArray("providedMeals");
        if (breakfastProvided) {
            providedMeals.add("BREAKFAST");
        }
        if (lunchProvided) {
            providedMeals.add("LUNCH");
        }
        if (dinnerProvided) {
            providedMeals.add("DINNER");
        }
        putAmount(meals, "deductionAmount", MEAL_DEDUCTION, text);
        if (mealProvided) {
            evidence(evidenceRefs, ocrText, "meals.provided", "식사 제공 여부: 제공([CHECKED]중식)", "식사 제공");
        }
    }

    private void addSignature(ObjectNode contractTerms, ArrayNode evidenceRefs, OcrText ocrText, String text) {
        ObjectNode signature = contractTerms.putObject("signature");
        Matcher matcher = SIGNED_DATE.matcher(text);
        if (matcher.find()) {
            signature.put("status", "FOUND");
            signature.put("signedDate", LocalDate.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            ).toString());
            evidence(evidenceRefs, ocrText, "signature.signedDate", "[DATE]", matcher.group());
        } else {
            signature.put("status", "MISSING");
        }
        signature.put("employerSignaturePresent", contains(text, "사용자:") || containsLower(text, "employer :"));
        signature.put("workerSignaturePresent", contains(text, "근로자 :"));
    }

    private void addChecklistCodes(ArrayNode checklistCodes) {
        List.of(
                "FEA_STANDARD_EMPLOYMENT_CONTRACT",
                "LRA_WRITTEN_CONDITIONS",
                "LRA_MINIMUM_WAGE",
                "LRA_DIRECT_FULL_WAGE_PAYMENT",
                "LRA_REGULAR_PAYDAY",
                "LRA_OVERTIME_NIGHT_HOLIDAY_PREMIUM",
                "LRA_STATUTORY_WORKING_HOURS",
                "LRA_REST_BREAKS",
                "LRA_WEEKLY_PAID_HOLIDAY",
                "FEA_DORMITORY_INFO_DISCLOSURE",
                "FEA_DORMITORY_STANDARD"
        ).forEach(checklistCodes::add);
    }

    private String reviewRequiredReason(ObjectNode contractTerms) {
        List<String> missing = new ArrayList<>();
        if ("MISSING".equals(contractTerms.path("contractPeriod").path("status").asText())) {
            missing.add("contractPeriod");
        }
        if ("MISSING".equals(contractTerms.path("wage").path("status").asText())) {
            missing.add("wage");
        }
        if ("MISSING".equals(contractTerms.path("workingHours").path("status").asText())) {
            missing.add("workingHours");
        }
        if ("MISSING".equals(contractTerms.path("breakTime").path("status").asText())) {
            missing.add("breakTime");
        }
        return missing.isEmpty() ? null : "Missing required fields: " + String.join(", ", missing);
    }

    private void evidence(ArrayNode evidenceRefs, OcrText ocrText, String fieldName, String maskedExcerpt, String... needles) {
        Optional<TextBlockEvidence> source = ocrText.findEvidence(needles);
        ObjectNode evidence = evidenceRefs.addObject();
        evidence.put("evidenceId", "ev-" + evidenceRefs.size());
        evidence.put("fieldName", fieldName);
        evidence.put("page", source.map(TextBlockEvidence::page).orElse(1));
        ObjectNode box = evidence.putObject("boundingBox");
        BoundingBox boundingBox = source.map(TextBlockEvidence::boundingBox).orElse(BoundingBox.empty());
        box.put("x", boundingBox.x());
        box.put("y", boundingBox.y());
        box.put("width", boundingBox.width());
        box.put("height", boundingBox.height());
        evidence.put("confidence", source.map(TextBlockEvidence::confidence).orElse(0.8));
        evidence.put("maskedExcerpt", maskedExcerpt);
    }

    private record OcrText(String text, List<TextBlockEvidence> evidence) {
        Optional<TextBlockEvidence> findEvidence(String... needles) {
            if (needles == null || needles.length == 0) {
                return Optional.empty();
            }
            for (String needle : needles) {
                if (needle == null || needle.isBlank()) {
                    continue;
                }
                Optional<TextBlockEvidence> matched = evidence.stream()
                        .filter(item -> item.text().contains(needle))
                        .findFirst();
                if (matched.isPresent()) {
                    return matched;
                }
            }
            return Optional.empty();
        }
    }

    private record TextBlockEvidence(String text, int page, JsonNode bboxNode, double confidence) {
        BoundingBox boundingBox() {
            return BoundingBox.from(bboxNode);
        }
    }

    private record BoundingBox(double x, double y, double width, double height) {
        static BoundingBox empty() {
            return new BoundingBox(0, 0, 0, 0);
        }

        static BoundingBox from(JsonNode node) {
            if (!node.isArray() || node.size() < 4) {
                return empty();
            }
            double x1 = node.get(0).asDouble();
            double y1 = node.get(1).asDouble();
            double third = node.get(2).asDouble();
            double fourth = node.get(3).asDouble();
            double width = third > x1 ? third - x1 : third;
            double height = fourth > y1 ? fourth - y1 : fourth;
            return new BoundingBox(x1, y1, Math.max(width, 0), Math.max(height, 0));
        }
    }

    private boolean contains(String text, String value) {
        return text.contains(value);
    }

    private boolean containsLower(String text, String value) {
        return text.toLowerCase().contains(value.toLowerCase());
    }

    private boolean checkedNear(String text, String value) {
        String quoted = Pattern.quote(value);
        return Pattern.compile("[\\[\\(]?\\s*√\\s*[\\]\\)]?\\s*[^\\n]{0,24}" + quoted, Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find()
                || Pattern.compile(quoted + "[^\\n]{0,24}[\\[\\(]?\\s*√\\s*[\\]\\)]?", Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find();
    }

    private boolean checkedOption(String text, String value) {
        String quoted = Pattern.quote(value);
        return Pattern.compile("[\\[\\(]?\\s*√\\s*[\\]\\)]?\\s*" + quoted, Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find();
    }

    private boolean checkedAfter(String text, String anchor, String value, int maxChars) {
        int anchorIndex = text.toLowerCase().indexOf(anchor.toLowerCase());
        if (anchorIndex < 0) {
            return false;
        }
        String optionWindow = text.substring(anchorIndex, Math.min(text.length(), anchorIndex + maxChars));
        return checkedOption(optionWindow, value);
    }

    private String paymentMethod(String text) {
        if (checkedOption(text, "직접 지급") || checkedOption(text, "1직접 지급") || checkedOption(text, "In person")) {
            return "IN_PERSON";
        }
        if (checkedOption(text, "통장") || checkedOption(text, "By direct deposit") || checkedOption(text, "direct deposit")) {
            return "BANK_TRANSFER";
        }
        return containsLower(text, "direct deposit") ? "BANK_TRANSFER" : "UNKNOWN";
    }

    private String date(String yy, String month, String day) {
        int year = 2000 + Integer.parseInt(yy);
        return LocalDate.of(year, Integer.parseInt(month), Integer.parseInt(day)).toString();
    }

    private String twoDigits(String value) {
        return String.format("%02d", Integer.parseInt(value));
    }

    private String[] timeEvidenceCandidates(String matchedText, String startHour, String startMinute, String endHour, String endMinute) {
        String start = twoDigits(startHour) + ":" + twoDigits(startMinute);
        String end = twoDigits(endHour) + ":" + twoDigits(endMinute);
        String startHourTwoDigits = twoDigits(startHour);
        String endHourTwoDigits = twoDigits(endHour);
        return new String[]{
                matchedText,
                start,
                end,
                startHour + "시",
                endHour + "시",
                startHourTwoDigits + "시",
                endHourTwoDigits + "시",
                startHour + "人",
                endHour + "人",
                startHourTwoDigits + "人",
                endHourTwoDigits + "人"
        };
    }

    private void putInteger(ObjectNode node, String field, Pattern pattern, String text) {
        Integer value = findInteger(pattern, text);
        if (value != null) {
            node.put(field, value);
        }
    }

    private Integer findInteger(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        for (int index = 1; index <= matcher.groupCount(); index += 1) {
            String value = matcher.group(index);
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value);
            }
        }
        return null;
    }

    private void putAmount(ObjectNode node, String field, Pattern pattern, String text) {
        Integer value = findAmount(pattern, text);
        if (value != null) {
            node.put(field, value);
        }
    }

    private Integer findAmount(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        for (int index = 1; index <= matcher.groupCount(); index += 1) {
            String value = matcher.group(index);
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value.replace(",", "").replace("o", "0").replace("O", "0"));
            }
        }
        return null;
    }

    private void addAllowance(ArrayNode fixedAllowances, String type, Pattern pattern, String text) {
        Integer amount = findAmount(pattern, text);
        if (amount == null) {
            return;
        }
        ObjectNode allowance = fixedAllowances.addObject();
        allowance.put("type", type);
        allowance.put("amount", amount);
    }
}
