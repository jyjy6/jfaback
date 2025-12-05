package jy.Job_Flow_Agent.Member.Controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jy.Job_Flow_Agent.Auth.Util.AuthUtils;
import jy.Job_Flow_Agent.Member.DTO.MemberDto;
import jy.Job_Flow_Agent.Member.DTO.MemberFormDto;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import jy.Job_Flow_Agent.Member.Service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/member")
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "회원", description = "회원 정보 관련 API")
public class MemberController {
    private final MemberService memberService;


    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody MemberFormDto memberFormDto) {
        memberService.registerUser(memberFormDto);
        return new ResponseEntity<>("회원가입이 완료되었습니다", HttpStatus.CREATED);
    }


    
    @Operation(
            summary = "회원 정보 조회",
            description = "현재 인증된 사용자의 상세 회원 정보를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청(로그인 안됨)")
    })
    @GetMapping("/userinfo")
    public MemberDto getUserInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {

        AuthUtils.loginCheck(customUserDetails);
        return memberService.getUserInfo(customUserDetails);
    }



}
