package jy.Job_Flow_Agent.AI.DTO;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI가 채용공고에서 추출할 구조화된 데이터
 */


public record JobPostingInfo(
        @Description("회사 이름")
        String companyName,

        @Description("채용 공고 제목")
        String title,

        @Description("주요 업무 (Responsibilities) 요약")
        List<String> majorTasks,

        @Description("자격 요건 (Requirements) 요약")
        List<String> requirements,

        @Description("우대 사항 (Preferred Qualifications) 요약")
        List<String> preferredSkills,

        @Description("기술 스택 (예: Java, Spring, React, Python 등)")
        List<String> techStack,

        @Description("채용 마감일 (날짜가 없으면 '채용시까지' 또는 '상시채용')")
        String deadline,

        @Description("연봉 정보 (없으면 '회사 내규에 따름' 또는 '면접 후 협의')")
        String salary,

        @Description("근무지 위치")
        String location
) {}
