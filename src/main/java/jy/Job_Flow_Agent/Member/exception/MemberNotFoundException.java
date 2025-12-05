package jy.Job_Flow_Agent.Member.exception;


import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends GlobalException {
    public MemberNotFoundException(Long id) {
        super("ID " + id + " 에 해당하는 유저를 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public MemberNotFoundException(String username) {
        super("유저이름 " + username + " 에 해당하는 유저를 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
