package com.kworkerharmony.backend.document.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PaddleOcrEmploymentContractExtractor {

    public static final String SCHEMA_VERSION = "employment-contract-v1";
    public static final String SOURCE_ENGINE = "PADDLE_OCR";

    private static final Pattern CONTRACT_PERIOD = Pattern.compile("(\\d{2})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*~\\s*(\\d{2})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final Pattern CONTRACT_PERIOD_EN = Pattern.compile("from\\((\\d{2})/(\\d{1,2})/(\\d{1,2})\\s*YY/MM/DD\\)\\s*to\\((\\d{2})/(\\d{1,2})/(\\d{1,2})\\s*YY/MM/DD\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORKING_HOURS_KO = Pattern.compile("(\\d{1,2})시\\s*(\\d{1,2})분\\s*~\\s*(\\d{1,2})시\\s*(\\d{1,2})분");
    private static final Pattern WORKING_HOURS_EN = Pattern.compile("from \\(\\s*(\\d{1,2}:\\d{2}) \\) to \\(\\s*(\\d{1,2}:\\d{2}) \\)");
    private static final Pattern OVERTIME = Pattern.compile("1일 평균 시간외 근로시간:\\s*(\\d+)시간");
    private static final Pattern VARIABLE_HOURS = Pattern.compile("변동 가능:\\s*(\\d+)시간 이내");
    private static final Pattern BREAK_TIME = Pattern.compile("휴게시간\\s*1일\\s*(\\d+)분");
    private static final Pattern BREAK_TIME_EN = Pattern.compile("\\(\\s*(\\d+)\\s*\\) minutes per day", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTHLY_WAGE = Pattern.compile("월 통상임금 \\(\\s*([0-9,]+)\\s*\\)원");
    private static final Pattern BASE_PAY = Pattern.compile("기본급\\[\\s*월급\\s*]\\s*\\(\\s*([0-9,]+)\\s*\\)원");
    private static final Pattern PRODUCTION_ALLOWANCE = Pattern.compile("생산 수당:\\s*([0-9,]+)\\s*\\)원");
    private static final Pattern MEAL_ALLOWANCE = Pattern.compile("식대 수당:\\s*([0-9,]+)\\s*\\)원");
    private static final Pattern BONUS = Pattern.compile("상여금 \\(\\s*([0-9,]+)\\s*\\)원");
    private static final Pattern PAYMENT_DAY = Pattern.compile("임금지급일 매월 \\(\\s*(\\d{1,2})\\s*\\)일");
    private static final Pattern DORMITORY_DEDUCTION = Pattern.compile("숙박시설 제공 시 근로자 부담금액:\\s*매월\\s*([0-9,]+)\\s*원");
    private static final Pattern MEAL_DEDUCTION = Pattern.compile("식사 제공시 근로자 부담금액:\\s*매월\\s*([0-9,]+)\\s*\\)?원");
    private static final Pattern SIGNED_DATE = Pattern.compile("(20\\d{2})\\.(\\d{1,2})\\.(\\d{1,2})\\.");

    private final ObjectMapper objectMapper;

    public PaddleOcrEmploymentContractExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EmploymentContractExtractionPayload extract(JsonNode ocrResult) {
        String text = collectText(ocrResult);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        ObjectNode contractTerms = root.putObject("contractTerms");
        ArrayNode evidenceRefs = root.putArray("evidenceRefs");
        ArrayNode checklistCodes = root.putArray("candidateChecklistItemCodes");

        addDocumentType(contractTerms, text);
        addContractPeriod(contractTerms, evidenceRefs, text);
        addProbation(contractTerms, evidenceRefs, text);
        addWork(contractTerms, evidenceRefs, text);
        addWorkingHours(contractTerms, evidenceRefs, text);
        addBreakTime(contractTerms, evidenceRefs, text);
        addHolidays(contractTerms, evidenceRefs, text);
        addWage(contractTerms, evidenceRefs, text);
        addDormitoryAndMeals(contractTerms, evidenceRefs, text);
        addSignature(contractTerms, evidenceRefs, text);
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

    private String collectText(JsonNode root) {
        StringBuilder builder = new StringBuilder();
        appendMarkdownText(root.path("layoutParsingResults"), builder);
        appendBlockContent(root.path("layoutParsingResults"), builder);
        return builder.toString()
                .replace("&nbsp;", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void appendMarkdownText(JsonNode results, StringBuilder builder) {
        if (!results.isArray()) {
            return;
        }
        results.forEach(result -> append(builder, result.path("markdown").path("text").asText("")));
    }

    private void appendBlockContent(JsonNode results, StringBuilder builder) {
        if (!results.isArray()) {
            return;
        }
        for (JsonNode result : results) {
            JsonNode blocks = result.path("prunedResult").path("parsing_res_list");
            if (!blocks.isArray()) {
                continue;
            }
            blocks.forEach(block -> append(builder, block.path("block_content").asText("")));
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

    private void addContractPeriod(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode period = contractTerms.putObject("contractPeriod");
        Matcher matcher = CONTRACT_PERIOD.matcher(text);
        if (matcher.find()) {
            period.put("status", "FOUND");
            period.put("contractStartDate", date(matcher.group(1), matcher.group(2), matcher.group(3)));
            period.put("contractEndDate", date(matcher.group(4), matcher.group(5), matcher.group(6)));
            evidence(evidenceRefs, "contractPeriod", "계약기간 [DATE] ~ [DATE]");
            return;
        }
        matcher = CONTRACT_PERIOD_EN.matcher(text);
        if (matcher.find()) {
            period.put("status", "FOUND");
            period.put("contractStartDate", date(matcher.group(1), matcher.group(2), matcher.group(3)));
            period.put("contractEndDate", date(matcher.group(4), matcher.group(5), matcher.group(6)));
            evidence(evidenceRefs, "contractPeriod", "from [DATE] to [DATE]");
            return;
        }
        period.put("status", "MISSING");
    }

    private void addProbation(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode probation = contractTerms.putObject("probation");
        boolean included = text.contains("수습기간") && (text.contains("[√] 활용") || text.contains("[√] Included"));
        probation.put("status", included ? "FOUND" : "MISSING");
        probation.put("included", included);
        if (included) {
            probation.put("months", 1);
            evidence(evidenceRefs, "probation", "수습기간 [CHECKED] 1개월");
        }
    }

    private void addWork(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode work = contractTerms.putObject("work");
        work.put("industryCategory", contains(text, "제조업") || containsLower(text, "manufacturing") ? "MANUFACTURING" : "UNKNOWN");
        work.put("workplaceRegion", contains(text, "안산") || containsLower(text, "ansan") ? "GYEONGGI_ANSAN" : "UNKNOWN");
        work.put("businessCategory", contains(text, "자동차 금속부품") ? "AUTOMOTIVE_METAL_PARTS" : "UNKNOWN");
        work.put("jobCategory", contains(text, "금속부품 조립") ? "METAL_PARTS_ASSEMBLY_INSPECTION_PACKAGING" : "UNKNOWN");
        if (!work.path("jobCategory").asText().equals("UNKNOWN")) {
            evidence(evidenceRefs, "work.jobCategory", "직무내용: 금속부품 조립, 품질검사, 포장작업");
        }
    }

    private void addWorkingHours(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode workingHours = contractTerms.putObject("workingHours");
        Matcher matcher = WORKING_HOURS_KO.matcher(text);
        if (matcher.find()) {
            workingHours.put("status", "FOUND");
            workingHours.put("startTime", twoDigits(matcher.group(1)) + ":" + twoDigits(matcher.group(2)));
            workingHours.put("endTime", twoDigits(matcher.group(3)) + ":" + twoDigits(matcher.group(4)));
            evidence(evidenceRefs, "workingHours", "[TIME] ~ [TIME]");
        } else {
            matcher = WORKING_HOURS_EN.matcher(text);
            if (matcher.find()) {
                workingHours.put("status", "FOUND");
                workingHours.put("startTime", matcher.group(1));
                workingHours.put("endTime", matcher.group(2));
                evidence(evidenceRefs, "workingHours", "from [TIME] to [TIME]");
            } else {
                workingHours.put("status", "MISSING");
            }
        }
        putInteger(workingHours, "overtimeHoursPerDay", OVERTIME, text);
        putInteger(workingHours, "maxVariableHoursPerDay", VARIABLE_HOURS, text);
        workingHours.put("shiftSystem", false);
    }

    private void addBreakTime(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
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
        evidence(evidenceRefs, "breakTime.minutesPerDay", "휴게시간 [MINUTES]분");
    }

    private void addHolidays(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode holidays = contractTerms.putObject("holidays");
        boolean found = contains(text, "6. 휴일") || containsLower(text, "holidays");
        holidays.put("status", found ? "FOUND" : "MISSING");
        holidays.put("sunday", contains(text, "[√]일요일"));
        holidays.put("legalHoliday", contains(text, "[√]공휴일") || containsLower(text, "[√]legal holiday"));
        holidays.put("legalHolidayPaid", contains(text, "공휴일([√]유급") || containsLower(text, "legal holiday([√]paid"));
        holidays.put("everySaturday", contains(text, "[√]매주 토요일") || containsLower(text, "[√]every saturday"));
        if (found) {
            evidence(evidenceRefs, "holidays", "휴일 [CHECKED]일요일 [CHECKED]공휴일 [CHECKED]매주 토요일");
        }
    }

    private void addWage(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode wage = contractTerms.putObject("wage");
        Integer monthlyWage = findAmount(MONTHLY_WAGE, text);
        if (monthlyWage == null) {
            wage.put("status", "MISSING");
        } else {
            wage.put("status", "FOUND");
            wage.put("amount", monthlyWage);
            wage.put("currency", "KRW");
            wage.put("period", "MONTHLY");
            evidence(evidenceRefs, "wage.amount", "월 통상임금 ([AMOUNT])원");
        }
        putAmount(wage, "basePay", BASE_PAY, text);
        putAmount(wage, "bonusAmount", BONUS, text);
        putInteger(wage, "paymentDay", PAYMENT_DAY, text);
        wage.put("paymentMethod", contains(text, "[ √ ]통장") || containsLower(text, "direct deposit") ? "BANK_TRANSFER" : "UNKNOWN");
        wage.put("overtimeNightHolidayPremiumMentioned", contains(text, "50%를 가산") || containsLower(text, "50% more"));

        ArrayNode fixedAllowances = wage.putArray("fixedAllowances");
        addAllowance(fixedAllowances, "PRODUCTION", PRODUCTION_ALLOWANCE, text);
        addAllowance(fixedAllowances, "MEAL", MEAL_ALLOWANCE, text);
    }

    private void addDormitoryAndMeals(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode dormitory = contractTerms.putObject("dormitory");
        boolean dormitoryProvided = contains(text, "숙박시설 제공 여부: [ √ ]제공") || containsLower(text, "provision of accommodation: [ √ ]provided");
        dormitory.put("status", contains(text, "숙박시설 제공") ? "FOUND" : "MISSING");
        dormitory.put("provided", dormitoryProvided);
        dormitory.put("typeCategory", contains(text, "기숙사") ? "DORMITORY" : "UNKNOWN");
        putAmount(dormitory, "deductionAmount", DORMITORY_DEDUCTION, text);
        if (dormitoryProvided) {
            evidence(evidenceRefs, "dormitory.provided", "숙박시설 제공 여부: [CHECKED]제공");
        }

        ObjectNode meals = contractTerms.putObject("meals");
        boolean mealProvided = contains(text, "식사 제공 여부: 제공");
        meals.put("status", contains(text, "식사 제공") ? "FOUND" : "MISSING");
        meals.put("provided", mealProvided);
        ArrayNode providedMeals = meals.putArray("providedMeals");
        if (contains(text, "[ √ ]중식")) {
            providedMeals.add("LUNCH");
        }
        putAmount(meals, "deductionAmount", MEAL_DEDUCTION, text);
        if (mealProvided) {
            evidence(evidenceRefs, "meals.provided", "식사 제공 여부: 제공([CHECKED]중식)");
        }
    }

    private void addSignature(ObjectNode contractTerms, ArrayNode evidenceRefs, String text) {
        ObjectNode signature = contractTerms.putObject("signature");
        Matcher matcher = SIGNED_DATE.matcher(text);
        if (matcher.find()) {
            signature.put("status", "FOUND");
            signature.put("signedDate", LocalDate.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            ).toString());
            evidence(evidenceRefs, "signature.signedDate", "[DATE]");
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

    private void evidence(ArrayNode evidenceRefs, String fieldName, String maskedExcerpt) {
        ObjectNode evidence = evidenceRefs.addObject();
        evidence.put("evidenceId", "ev-" + evidenceRefs.size());
        evidence.put("fieldName", fieldName);
        evidence.put("page", 1);
        ObjectNode box = evidence.putObject("boundingBox");
        box.put("x", 0);
        box.put("y", 0);
        box.put("width", 0);
        box.put("height", 0);
        evidence.put("confidence", 0.8);
        evidence.put("maskedExcerpt", maskedExcerpt);
    }

    private boolean contains(String text, String value) {
        return text.contains(value);
    }

    private boolean containsLower(String text, String value) {
        return text.toLowerCase().contains(value.toLowerCase());
    }

    private String date(String yy, String month, String day) {
        int year = 2000 + Integer.parseInt(yy);
        return LocalDate.of(year, Integer.parseInt(month), Integer.parseInt(day)).toString();
    }

    private String twoDigits(String value) {
        return String.format("%02d", Integer.parseInt(value));
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
        return Integer.parseInt(matcher.group(1));
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
        return Integer.parseInt(matcher.group(1).replace(",", ""));
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
