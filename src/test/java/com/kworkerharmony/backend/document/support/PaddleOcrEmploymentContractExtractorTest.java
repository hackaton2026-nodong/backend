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
        assertThat(extracted.path("contractTerms").path("workingHours").path("startTime").asText()).isEqualTo("08:30");
        assertThat(extracted.path("contractTerms").path("breakTime").path("minutesPerDay").asInt()).isEqualTo(60);
        assertThat(extracted.path("contractTerms").path("dormitory").path("deductionAmount").asInt()).isEqualTo(150_000);
        assertThat(extracted.path("contractTerms").path("meals").path("providedMeals").get(0).asText()).isEqualTo("LUNCH");

        assertThat(payload.payloadJson()).doesNotContain("MARIA LUZ SANTOS");
        assertThat(payload.payloadJson()).doesNotContain("김민수");
        assertThat(payload.payloadJson()).doesNotContain("214-86-73951");
        assertThat(payload.payloadJson()).doesNotContain("031-555-1290");
        assertThat(payload.payloadJson()).doesNotContain("경기도 안산시 단원구 산단로 125");
        assertThat(payload.payloadJson()).doesNotContain("layoutParsingResults");
        assertThat(payload.payloadJson()).doesNotContain("markdown");
    }
}
