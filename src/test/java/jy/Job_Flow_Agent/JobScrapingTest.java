package jy.Job_Flow_Agent;


import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;




@Slf4j
@SpringBootTest
public class JobScrapingTest {

    // application-dev.properties에 설정된 키(google.gemini.api.key)를 우선 사용
    @Value("${openai.api.key}")
    private String geminiApiKey;

    
    @Test
    void testJobPostingScraping() {
        // 테스트하고 싶은 채용 공고 URL을 여기에 넣으세요
        String targetUrl = "https://www.jobkorea.co.kr/Recruit/GI_Read/48141243?Oem_Code=C1&logpath=1&stext=%EC%9D%BC%EB%B3%B8&listno=2&sc=630"; // 예시 URL (변경 필요)


        log.info(">>> URL 접속 시도: {}", targetUrl);

        try {
            // 1. Jsoup으로 HTML 가져오기 (User-Agent 설정 필수)
            Document doc = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

            // 2. 텍스트 추출 (HTML 태그 제거)
            String title = doc.title();
            String bodyText = doc.body().text();

            log.info(">>> 제목: {}", title);
            log.info(">>> 본문 길이: {}", bodyText.length());
            // 너무 기니까 앞부분만 출력
            log.info(">>> 본문 내용(일부분): {}...", bodyText.substring(0, Math.min(bodyText.length(), 500)));

            // 3. (선택사항) LangChain4j 연동 테스트
            // API 키가 있고 'dummy'가 아닐 때만 실행
            if (geminiApiKey != null && !geminiApiKey.equals("dummy") && !geminiApiKey.isEmpty()) {
                log.info(">>> Gemini에게 요약 요청 중...");

                ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                        .apiKey(geminiApiKey)
                        .modelName("gemini-2.5-flash")
                        .build();

                String prompt = "다음 채용 공고 내용을 3줄로 요약해줘:\n\n" + bodyText;
                ChatResponse chatResponse = model.chat(UserMessage.from(prompt));
                String response = chatResponse.aiMessage().text();

                log.info(">>> Gemini 응답:\n{}", response);
            } else {
                log.warn(">>> Gemini API Key가 설정되지 않아 LLM 요약은 건너뜁니다.");
            }

        } catch (IOException e) {
            log.error(">>> 크롤링 실패: {}", e.getMessage(), e);
        }
    }
}
