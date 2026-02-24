package jy.Job_Flow_Agent.ai;

import jy.Job_Flow_Agent.AI.Tools.MemberSearchTools;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.DTO.MemberDto;
import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSearchTools 단위 테스트")
class MemberSearchToolsTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberSearchTools memberSearchTools;

    private Member buildMember(String username) {
        return Member.builder()
                .id(1L)
                .username(username)
                .email(username + "@test.com")
                .displayName("닉네임")
                .password("pw")
                .roles(Set.of("ROLE_USER"))
                .build();
    }

    // ─────────────────────────────────────────────────
    //  MST-01: findMemberByUsername() 정상 조회
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MST-01: 존재하는 username 조회 → MemberDto 반환")
    void findMemberByUsername_exists_returnsMemberDto() {
        given(memberRepository.findByUsername("testuser")).willReturn(Optional.of(buildMember("testuser")));

        MemberDto result = memberSearchTools.findMemberByUsername("testuser");

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("testuser@test.com");
    }

    // ─────────────────────────────────────────────────
    //  MST-02: findMemberByUsername() 없는 사용자
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MST-02: 없는 username 조회 → GlobalException(USERNAME_ERROR) 발생")
    void findMemberByUsername_notFound_throwsGlobalException() {
        given(memberRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberSearchTools.findMemberByUsername("ghost"))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("USERNAME_ERROR"));
    }

    // ─────────────────────────────────────────────────
    //  MST-03: findMemberByEmail() 정상 조회
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MST-03: 존재하는 email 조회 → MemberDto 반환")
    void findMemberByEmail_exists_returnsMemberDto() {
        given(memberRepository.findByEmail("testuser@test.com")).willReturn(Optional.of(buildMember("testuser")));

        MemberDto result = memberSearchTools.findMemberByEmail("testuser@test.com");

        assertThat(result.getEmail()).isEqualTo("testuser@test.com");
    }

    @Test
    @DisplayName("MST-03: 없는 email 조회 → GlobalException(USER_EMAIL_ERROR) 발생")
    void findMemberByEmail_notFound_throwsGlobalException() {
        given(memberRepository.findByEmail("nobody@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberSearchTools.findMemberByEmail("nobody@test.com"))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("USER_EMAIL_ERROR"));
    }

    // ─────────────────────────────────────────────────
    //  MST-04: findMemberById() 정상 조회
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MST-04: 존재하는 id 조회 → MemberDto 반환")
    void findMemberById_exists_returnsMemberDto() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(buildMember("testuser")));

        MemberDto result = memberSearchTools.findMemberById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("MST-04: 없는 id 조회 → GlobalException(USER_ID_ERROR) 발생")
    void findMemberById_notFound_throwsGlobalException() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberSearchTools.findMemberById(999L))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("USER_ID_ERROR"));
    }
}
