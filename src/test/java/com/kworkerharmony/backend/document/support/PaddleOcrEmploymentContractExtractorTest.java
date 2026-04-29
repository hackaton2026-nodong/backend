package com.kworkerharmony.backend.document.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PaddleOcrEmploymentContractExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaddleOcrEmploymentContractExtractor extractor = new PaddleOcrEmploymentContractExtractor(objectMapper);

    @Test
    void extractsSanitizedEmploymentContractTermsFromPaddleOcrJson() throws Exception {
        JsonNode ocrResult = objectMapper.readTree("""
                {
                  "layoutParsingResults": [
                    {
                      "markdown": {
                        "text": "## 표준근로계약서 Standard Labor Contract\\n한국제조 031-555-1290 김민수 Identification number 214-86-73951 MARIA LUZ SANTOS 1998-07-21 -신규 또는 재입국자: (12) 개월, 26년 06월 01일 ~ 27년 05월 31일 - 수습기간: [√] 활용(입국일부터 [√] 1개월 [ ]2개월 [ ]3개월 [ ]개월) 2.근로장소 경기도 안산시 단원구 산단로 125 한국제조 제 1 공장 - 업종: 제조업 - 사업내용: 자동차 금속부품 생산 3.업무내용 - 직무내용: 금속부품 조립, 품질검사, 포장작업 08시 30분 ~ 17시 30분 -1일 평균 시간외 근로시간: 1시간 (사업장 사정에 따라 변동 가능: 2시간 이내) 5. 휴게시간 1일 60분 6. 휴일 [√]일요일 [√]공휴일([√]유급 [ ]무급) [√]매주 토요일 7. 임금 1) 월 통상임금 ( 2,300,000 )원- 기본급[ 월급 ] ( 2,150,000 )원- 고정적 수당: ( 생산 수당: 100,000 )원), ( 식대 수당: 50,000 )원)- 상여금 ( 0 )원) 8) 임금지급일 매월 ( 10 )일 9) 지급방법 [ ]직접 지급, [ √ ]통장 임금 1) 숙박시설 제공- 숙박시설 제공 여부: [ √ ]제공 [ ]미제공 기타주택형태 시설( 기숙사 ))10) 숙박시설 제공 시 근로자 부담금액: 매월 150,000 원2) 식사 제공- 식사 제공 여부: 제공([ ]조식, [ √ ]중식, [ ]석식), [ ]미제공- 식사 제공시 근로자 부담금액:매월 0 )원 2026.06.01. 사용자:김민수 근로자 : MARIA LUZ SANTOS"
                      }
                    }
                  ]
                }
                """);

        EmploymentContractExtractionPayload payload = extractor.extract(ocrResult);
        JsonNode extracted = objectMapper.readTree(payload.payloadJson());

        assertThat(payload.reviewRequiredReason()).isNull();
        assertThat(extracted.path("contractTerms").path("contractPeriod").path("contractStartDate").asText())
                .isEqualTo("2026-06-01");
        assertThat(extracted.path("contractTerms").path("contractPeriod").path("contractEndDate").asText())
                .isEqualTo("2027-05-31");
        assertThat(extracted.path("contractTerms").path("wage").path("amount").asInt()).isEqualTo(2_300_000);
        assertThat(extracted.path("contractTerms").path("wage").path("basePay").asInt()).isEqualTo(2_150_000);
        assertThat(extracted.path("contractTerms").path("wage").path("paymentDay").asInt()).isEqualTo(10);
        assertThat(extracted.path("contractTerms").path("wage").path("paymentMethod").asText()).isEqualTo("BANK_TRANSFER");
        assertThat(extracted.path("contractTerms").path("workingHours").path("startTime").asText()).isEqualTo("08:30");
        assertThat(extracted.path("contractTerms").path("workingHours").path("endTime").asText()).isEqualTo("17:30");
        assertThat(extracted.path("contractTerms").path("workingHours").path("maxVariableHoursPerDay").asInt()).isEqualTo(2);
        assertThat(extracted.path("contractTerms").path("breakTime").path("minutesPerDay").asInt()).isEqualTo(60);
        assertThat(extracted.path("contractTerms").path("holidays").path("otherHoliday").asBoolean()).isFalse();
        assertThat(extracted.path("contractTerms").path("dormitory").path("typeCategory").asText()).isEqualTo("DORMITORY");
        assertThat(extracted.path("contractTerms").path("dormitory").path("deductionAmount").asInt()).isEqualTo(150_000);
        assertThat(extracted.path("contractTerms").path("meals").path("provided").asBoolean()).isTrue();
        assertThat(extracted.path("contractTerms").path("meals").path("notProvided").asBoolean()).isFalse();
        assertThat(extracted.path("contractTerms").path("meals").path("providedMeals").get(0).asText()).isEqualTo("LUNCH");

        assertThat(payload.payloadJson()).doesNotContain("MARIA LUZ SANTOS");
        assertThat(payload.payloadJson()).doesNotContain("김민수");
        assertThat(payload.payloadJson()).doesNotContain("214-86-73951");
        assertThat(payload.payloadJson()).doesNotContain("031-555-1290");
        assertThat(payload.payloadJson()).doesNotContain("경기도 안산시 단원구 산단로 125");
        assertThat(payload.payloadJson()).doesNotContain("layoutParsingResults");
        assertThat(payload.payloadJson()).doesNotContain("markdown");
    }

    @Test
    void extractsAllAvailableFieldsFromAlternateLaborContractOcrText() throws Exception {
        JsonNode ocrResult = objectMapper.readTree("""
                {
                  "layoutParsingResults": [
                    {
                      "markdown": {
                        "text": "## 표준근로계약서 Standard Labor Contract\\n한국제조 031-555-1290 경기도안산시단원구산단로125 Identification number 214-86-73951 MARIA LUZ SANTOS 1998-07-21 -신규 또는 재입국자:(12) 개월,26년 06 월 01일 ~27년 05 월 31일 -수습기간: [√]활용(입국일부터 [ ]1 개월 [ ]2 개월[ ]3개월 [6]개월) [ ]미활용 2. 근로장소 한국제조 본사 공장동 및 타사업장 지원 -업종:제조업 -사업내용: 공장업무 전반 3. 업무내용 -직무내용:기타 사용자 지시업무 07시 00분 ~ 20시 00분 -1일 평균 시간외 근로시간: 4시간 (사업장 사정에 따라 변동 가능: 5시간 이내) 5.휴게시간 1일 30분 6.휴일 [ ]일요일 [ ]공휴일([ ]유급 []무급)[ ]매주 토요일[]격주 토요일 [√]기타( 격주 일요일 (2주 1회) ) [ ]Every other Saturday [ √ ]etc.( Sunday off twice a month ) 7. 임금 1)월 통상임금 ( 1,850,000 )원 -기본급[ 월급 ] ( 1,850,000 )원 -고정적 수당:(생산 수당: 0 원), ( 식대 수당: 0 원) -상여금( 0원) 8.임금지급일 매월( 말 )일 Every ( last ) day of the month 9. 지급방법 [√]직접 지급, [ ]통장 입금 10.숙식제공 1)숙박시설 제공 -숙박시설 제공 여부: [√]제공 [ ]미제공 [√ ]컨테이너,[ ]조립식 패널,[ ]사업장 건물,기타주택형태 시설( 기숙사 )) -숙박시설제공 시근로자부담금액:매월 450,000원 2)식사 제공 -식사 제공 여부:제공([ ]조식,[ ]중식,[ ]석식), [ √ ]미제공 -식사제공시 근로자 부담금액:매월 원 2026. 06. 01. 사용자:김민수 Employer : Kim Minsu 근로자 : MARIA LUZ SANTOS Employee: MARIA LUZ SANTOS"
                      }
                    }
                  ]
                }
                """);

        EmploymentContractExtractionPayload payload = extractor.extract(ocrResult);
        JsonNode terms = objectMapper.readTree(payload.payloadJson()).path("contractTerms");

        assertThat(payload.reviewRequiredReason()).isNull();
        assertThat(terms.path("contractPeriod").path("contractStartDate").asText()).isEqualTo("2026-06-01");
        assertThat(terms.path("contractPeriod").path("contractEndDate").asText()).isEqualTo("2027-05-31");
        assertThat(terms.path("probation").path("status").asText()).isEqualTo("FOUND");
        assertThat(terms.path("probation").path("included").asBoolean()).isTrue();
        assertThat(terms.path("probation").path("months").asInt()).isEqualTo(6);
        assertThat(terms.path("work").path("industryCategory").asText()).isEqualTo("MANUFACTURING");
        assertThat(terms.path("work").path("workplaceRegion").asText()).isEqualTo("GYEONGGI_ANSAN");
        assertThat(terms.path("work").path("businessCategory").asText()).isEqualTo("OVERALL_FACTORY_WORK");
        assertThat(terms.path("work").path("jobCategory").asText()).isEqualTo("OTHER_USER_INSTRUCTED_TASKS");
        assertThat(terms.path("workingHours").path("startTime").asText()).isEqualTo("07:00");
        assertThat(terms.path("workingHours").path("endTime").asText()).isEqualTo("20:00");
        assertThat(terms.path("workingHours").path("overtimeHoursPerDay").asInt()).isEqualTo(4);
        assertThat(terms.path("workingHours").path("maxVariableHoursPerDay").asInt()).isEqualTo(5);
        assertThat(terms.path("breakTime").path("minutesPerDay").asInt()).isEqualTo(30);
        assertThat(terms.path("holidays").path("sunday").asBoolean()).isFalse();
        assertThat(terms.path("holidays").path("legalHoliday").asBoolean()).isFalse();
        assertThat(terms.path("holidays").path("everySaturday").asBoolean()).isFalse();
        assertThat(terms.path("holidays").path("otherHoliday").asBoolean()).isTrue();
        assertThat(terms.path("holidays").path("otherHolidayDescription").asText()).isEqualTo("SUNDAY_TWICE_A_MONTH");
        assertThat(terms.path("wage").path("amount").asInt()).isEqualTo(1_850_000);
        assertThat(terms.path("wage").path("basePay").asInt()).isEqualTo(1_850_000);
        assertThat(terms.path("wage").path("bonusAmount").asInt()).isZero();
        assertThat(terms.path("wage").path("paymentDayType").asText()).isEqualTo("MONTH_END");
        assertThat(terms.path("wage").path("paymentMethod").asText()).isEqualTo("IN_PERSON");
        assertThat(terms.path("wage").path("fixedAllowances").get(0).path("amount").asInt()).isZero();
        assertThat(terms.path("wage").path("fixedAllowances").get(1).path("amount").asInt()).isZero();
        assertThat(terms.path("dormitory").path("provided").asBoolean()).isTrue();
        assertThat(terms.path("dormitory").path("typeCategory").asText()).isEqualTo("CONTAINER_BOX");
        assertThat(terms.path("dormitory").path("deductionAmount").asInt()).isEqualTo(450_000);
        assertThat(terms.path("meals").path("provided").asBoolean()).isFalse();
        assertThat(terms.path("meals").path("notProvided").asBoolean()).isTrue();
        assertThat(terms.path("meals").path("providedMeals")).isEmpty();
        assertThat(terms.path("signature").path("status").asText()).isEqualTo("FOUND");
        assertThat(terms.path("signature").path("signedDate").asText()).isEqualTo("2026-06-01");
        assertThat(terms.path("signature").path("employerSignaturePresent").asBoolean()).isTrue();
        assertThat(terms.path("signature").path("workerSignaturePresent").asBoolean()).isTrue();
    }

    @Test
    void usesPaddleOcrBlockEvidenceForBoundingBoxesAndConfidence() throws Exception {
        JsonNode ocrResult = objectMapper.readTree("""
                {
                  "layoutParsingResults": [
                    {
                      "page": 2,
                      "prunedResult": {
                        "parsing_res_list": [
                          {
                            "page": 2,
                            "block_content": "26년 06월 01일 ~ 27년 05월 31일",
                            "block_bbox": [10, 20, 210, 60],
                            "confidence": 0.93
                          },
                          {
                            "page": 2,
                            "block_content": "08시 30분 ~ 17시 30분 5. 휴게시간 1일 60분 월 통상임금 ( 2,300,000 )원",
                            "block_bbox": [15, 70, 260, 130],
                            "confidence": 0.88
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        EmploymentContractExtractionPayload payload = extractor.extract(ocrResult);
        JsonNode extracted = objectMapper.readTree(payload.payloadJson());
        JsonNode contractPeriodEvidence = extracted.path("evidenceRefs").get(0);

        assertThat(contractPeriodEvidence.path("fieldName").asText()).isEqualTo("contractPeriod");
        assertThat(contractPeriodEvidence.path("page").asInt()).isEqualTo(2);
        assertThat(contractPeriodEvidence.path("boundingBox").path("x").asDouble()).isEqualTo(10);
        assertThat(contractPeriodEvidence.path("boundingBox").path("width").asDouble()).isEqualTo(200);
        assertThat(contractPeriodEvidence.path("confidence").asDouble()).isEqualTo(0.93);
    }

    @Test
    void usesSplitTimeTokenAsWorkingHoursEvidenceWhenFullTextIsNotInOneBlock() throws Exception {
        JsonNode ocrResult = objectMapper.readTree("""
                {
                  "layoutParsingResults": [
                    {
                      "markdown": {
                        "text": "표준근로계약서 26년 06월 01일 ~ 27년 05월 31일 07시 00분 ~ 20시 00분 5.휴게시간 1일 30분 월 통상임금 ( 1,850,000 )원"
                      },
                      "prunedResult": {
                        "parsing_res_list": [
                          {
                            "page": 1,
                            "block_content": "07시",
                            "block_bbox": [294, 1147, 349, 1172],
                            "confidence": 0.98
                          },
                          {
                            "page": 1,
                            "block_content": "20시",
                            "block_bbox": [556, 1147, 612, 1172],
                            "confidence": 0.97
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        EmploymentContractExtractionPayload payload = extractor.extract(ocrResult);
        JsonNode evidenceRefs = objectMapper.readTree(payload.payloadJson()).path("evidenceRefs");
        JsonNode workingHoursEvidence = null;
        for (JsonNode evidenceRef : evidenceRefs) {
            if ("workingHours".equals(evidenceRef.path("fieldName").asText())) {
                workingHoursEvidence = evidenceRef;
                break;
            }
        }

        assertThat(workingHoursEvidence).isNotNull();
        assertThat(workingHoursEvidence.path("boundingBox").path("x").asDouble()).isEqualTo(294);
        assertThat(workingHoursEvidence.path("boundingBox").path("width").asDouble()).isEqualTo(55);
        assertThat(workingHoursEvidence.path("confidence").asDouble()).isEqualTo(0.98);
    }

    @Test
    void extractsWorkingHoursAcrossCommonOcrTimeFormats() throws Exception {
        JsonNode ocrResult = objectMapper.readTree("""
                {
                  "layoutParsingResults": [
                    {
                      "markdown": {
                        "text": "표준근로계약서 26년 06월 01일 ~ 27년 05월 31일 from ( 08:30 ) to( 17:30 ) 08人 30是 ~ 17人 30是 5.휴게시간 1일 60분 월 통상임금 ( 2,300,000 )원"
                      },
                      "prunedResult": {
                        "parsing_res_list": [
                          {
                            "page": 1,
                            "block_content": "08人",
                            "block_bbox": [294, 1147, 349, 1172],
                            "confidence": 0.92
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        EmploymentContractExtractionPayload payload = extractor.extract(ocrResult);
        JsonNode extracted = objectMapper.readTree(payload.payloadJson());
        JsonNode workingHours = extracted.path("contractTerms").path("workingHours");

        assertThat(workingHours.path("status").asText()).isEqualTo("FOUND");
        assertThat(workingHours.path("startTime").asText()).isEqualTo("08:30");
        assertThat(workingHours.path("endTime").asText()).isEqualTo("17:30");
    }
}
