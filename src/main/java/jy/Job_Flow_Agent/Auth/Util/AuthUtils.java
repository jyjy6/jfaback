package jy.Job_Flow_Agent.Auth.Util;

import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

public class AuthUtils {

    /**
     * 유저가 보낸 id와 로그인된 실제id를 검증합니다
     * */
    public static void validateMemberId(Long requestId, CustomUserDetails userDetails) {
        if (!requestId.equals(userDetails.getId())) {
            throw new GlobalException("유저정보가 다릅니다",
                    "USER_INFORM_INCORRECT",
                    HttpStatus.FORBIDDEN);
        }
    }

    /**
     * 로그인했는지 확인함.
     * 로그인 안됐을 시 GlobalException("로그인이 필요합니다", "LOGIN_REQUIRED", HttpStatus.UNAUTHORIZED);
     * */
    public static boolean loginCheck(CustomUserDetails userDetails){
        if (userDetails == null || userDetails.getId() == null) {
            throw new GlobalException("로그인이 필요합니다", "LOGIN_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return true;
    }
}