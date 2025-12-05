package jy.Job_Flow_Agent.AI.Tools;


import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class UtilTools {

    @Tool("오늘 날짜(LocalDate)를 반환합니다. 형식은 'yyyy-MM-dd' 입니다.")
    public LocalDate getTodayDate() {
        LocalDate today = LocalDate.now();
        log.info("툴 호출: getTodayDate - today: {}", today);
        return today;
    }
}