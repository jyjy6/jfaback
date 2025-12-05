package jy.Job_Flow_Agent.Admin;



import jy.Job_Flow_Agent.AI.AssistantModels.Assistant;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Qualifier("assistantWithToolsForAdmin")
    private final Assistant assistantWithToolsForAdmin;


    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String message = request.get("message");
        log.info("Admin chat message from {}: {}", customUserDetails.getUsername(), message);

        return assistantWithToolsForAdmin.chat(customUserDetails.getId(), customUserDetails.getUsername(), message);
    }


    @PostMapping("/stream")
    public String streamChat(@RequestBody Map<String, String> request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String message = request.get("message");
        log.info("Admin stream chat message from {}: {}", customUserDetails.getUsername(), message);


        return assistantWithToolsForAdmin.chat(customUserDetails.getId() ,customUserDetails.getUsername(), message);
    }




}
