package jy.Job_Flow_Agent.AI.Tools;


import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.DTO.MemberDto;
import jy.Job_Flow_Agent.Member.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LangChain4j 툴: 회원 검색 및 조회 기능
 * 
 * AI 어시스턴트가 회원 정보를 조회하고 검증할 수 있도록 하는 툴 모음
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MemberSearchTools {
    private final MemberRepository memberRepository;

    /**
     * 사용자 이름(username)으로 회원 정보를 조회합니다.
     * 
     * @param username 조회할 회원의 로그인 아이디
     * @return 회원 정보 (회원이 존재하는 경우), null (존재하지 않는 경우)
     */
    @Tool("사용자 이름(로그인 아이디)으로 회원 정보를 조회합니다. 회원이 존재하면 기본 정보를 반환하고, 존재하지 않으면 null을 반환합니다.")
    public MemberDto findMemberByUsername(
            @P("조회할 회원의 로그인 아이디 (username)") String username
    ) {
        log.info("툴 호출: findMemberByUsername - username: {}", username);
        
        return memberRepository.findByUsername(username)
                .map(MemberDto::convertToDetailMemberDto)
                .orElseThrow(()-> new GlobalException("아이디가 잘못됐습니다", "USERNAME_ERROR", HttpStatus.BAD_REQUEST));

    }


    /**
     * 이메일 주소로 회원 정보를 조회합니다.
     * 
     * @param email 조회할 회원의 이메일 주소
     * @return 회원 정보 (회원이 존재하는 경우), null (존재하지 않는 경우)
     */
    @Tool("이메일 주소로 회원 정보를 조회합니다. 회원이 존재하면 기본 정보를 반환하고, 존재하지 않으면 null을 반환합니다.")
    public MemberDto findMemberByEmail(
            @P("조회할 회원의 이메일 주소") String email
    ) {
        log.info("툴 호출: findMemberByEmail - email: {}", email);
        return memberRepository.findByEmail(email)
                .map(MemberDto::convertToDetailMemberDto)
                .orElseThrow(()-> new GlobalException("이메일이 잘못됐습니다", "USER_EMAIL_ERROR", HttpStatus.BAD_REQUEST));
    }

    /**
     * 회원 ID로 회원 정보를 조회합니다.
     * 
     * @param memberId 조회할 회원의 고유 식별자(ID)
     * @return 회원 정보 (회원이 존재하는 경우), null (존재하지 않는 경우)
     */
    @Tool("회원 ID(고유 식별자)로 회원 정보를 조회합니다. 회원이 존재하면 기본 정보를 반환하고, 존재하지 않으면 null을 반환합니다.")
    public MemberDto findMemberById(
            @P("조회할 회원의 고유 식별자(ID), 숫자 형태") Long memberId
    ) {
        log.info("툴 호출: findMemberById - memberId: {}", memberId);
        
        return memberRepository.findById(memberId)
                .map(MemberDto::convertToDetailMemberDto)
                .orElseThrow(()-> new GlobalException("회원 고유 식별자가 잘못됐습니다", "USER_ID_ERROR", HttpStatus.BAD_REQUEST));
    }


}
