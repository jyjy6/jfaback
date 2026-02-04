package jy.Job_Flow_Agent.AI.Service;


import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Slf4j
@Service
public class JobScrappingService {

    public jobScrappingDTO jobScrapping(String URL){
        if(URL.isEmpty() || URL.equals("")){
            throw new GlobalException("URL을 입력하세요", "NEED_URL_LINK", HttpStatus.BAD_REQUEST);
        }
        try{
            // 1. Jsoup으로 HTML 가져오기 (User-Agent 설정 필수)
            Document doc = Jsoup.connect(URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

            return jobScrappingDTO.builder()
                    .title(doc.title())
                    .bodyText(doc.body().text())
                    .build();

        } catch (Exception e){
            log.error("크롤링 실패{}", e.getMessage(), e);
            throw new GlobalException("JOB_SCRAPPING_ERROR", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class jobScrappingDTO{
        private String title;
        private String bodyText;
    }


}
