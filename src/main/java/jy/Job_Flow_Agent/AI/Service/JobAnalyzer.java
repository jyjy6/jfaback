package jy.Job_Flow_Agent.AI.Service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import jy.Job_Flow_Agent.AI.DTO.JobPostingInfo;

/**
 * 채용공고 텍스트 분석기
 * LangChain4j의 @AiService를 통해(여기선 Langchain4j) 비정형 텍스트를 정형 데이터(JobPostingInfo)로 변환
 */
public interface JobAnalyzer {

    @SystemMessage("""
            당신은 채용공고 분석 전문가입니다.
            제공된 웹페이지 텍스트(HTML Body)에서 핵심 채용 정보를 추출하여 구조화된 데이터로 반환하세요.
            
            [지침]
            1. 불필요한 메뉴, 광고, 사이드바 내용은 무시하고 '실제 채용 공고 내용'에만 집중하세요.
            2. 기술 스택은 영어 명칭(예: Java, Python)으로 명확히 추출하세요.
            3. 정보가 명시되어 있지 않은 경우 null 대신 '정보 없음' 또는 문맥에 맞는 기본값(예: '상시채용')을 사용하세요.
            4. 주요 업무, 자격 요건, 우대 사항은 핵심 내용을 요약하여 리스트로 만드세요.
            5. 쓸데없는 말은 생략하고 결과만 return하세요.
            """)
    JobPostingInfo analyze(@UserMessage String rawHtmlContent);
}
