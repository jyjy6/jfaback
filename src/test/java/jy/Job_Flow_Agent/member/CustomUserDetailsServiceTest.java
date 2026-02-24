package jy.Job_Flow_Agent.member;

import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Repository.MemberRepository;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 단위 테스트")
class CustomUserDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private Member buildMember(String username) {
        return Member.builder()
                .id(1L)
                .username(username)
                .password("encoded-pw")
                .email(username + "@test.com")
                .displayName("닉네임")
                .roles(Set.of("ROLE_USER"))
                .build();
    }

    // ─────────────────────────────────────────────────
    //  CU-01: username으로 정상 조회 → CustomUserDetails 반환, lastLogin 업데이트
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("CU-01: username으로 loadUserByUsername() → CustomUserDetails 반환, lastLogin 업데이트")
    void loadUserByUsername_byUsername_returnsCustomUserDetails() {
        // given
        Member member = buildMember("testuser");
        given(memberRepository.findByUsername("testuser")).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willReturn(member);

        // when
        CustomUserDetails result = (CustomUserDetails) customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");

        // lastLogin 업데이트 확인
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        then(memberRepository).should().save(captor.capture());
        assertThat(captor.getValue().getLastLogin()).isNotNull();
        assertThat(captor.getValue().getLoginAttempts()).isEqualTo(0);
        assertThat(captor.getValue().getLoginSuspendedTime()).isNull();
    }

    // ─────────────────────────────────────────────────
    //  CU-01 (email fallback): email로 정상 조회
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("CU-01 (email): username 조회 실패 시 email로 재시도 → CustomUserDetails 반환")
    void loadUserByUsername_byEmail_returnsCustomUserDetails() {
        // given
        Member member = buildMember("testuser");
        given(memberRepository.findByUsername("testuser@test.com")).willReturn(Optional.empty());
        given(memberRepository.findByEmail("testuser@test.com")).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willReturn(member);

        // when
        CustomUserDetails result = (CustomUserDetails) customUserDetailsService.loadUserByUsername("testuser@test.com");

        // then
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    // ─────────────────────────────────────────────────
    //  CU-02: 존재하지 않는 사용자 → UsernameNotFoundException
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("CU-02: username/email 모두 없을 때 → UsernameNotFoundException 발생")
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        // given
        given(memberRepository.findByUsername("nobody")).willReturn(Optional.empty());
        given(memberRepository.findByEmail("nobody")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nobody");
    }
}
