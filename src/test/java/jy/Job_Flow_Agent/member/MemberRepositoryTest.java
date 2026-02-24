package jy.Job_Flow_Agent.member;

import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("MemberRepository 단위 테스트 (TestContainers MySQL)")
class MemberRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpw");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQL8Dialect");
    }

    @Autowired
    private MemberRepository memberRepository;

    private Member savedMember;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        savedMember = memberRepository.save(Member.builder()
                .username("testuser")
                .password("encoded-pw")
                .email("testuser@test.com")
                .displayName("테스트닉")
                .roles(Set.of("ROLE_USER"))
                .build());
    }

    // ─────────────────────────────────────────────────
    //  MR-01: findByUsername() - 존재하는 username
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MR-01: findByUsername() - 존재하는 username → Optional<Member> 값 있음")
    void findByUsername_exists_returnsOptionalWithMember() {
        Optional<Member> result = memberRepository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("MR-01: findByUsername() - 없는 username → Optional.empty()")
    void findByUsername_notExists_returnsEmpty() {
        Optional<Member> result = memberRepository.findByUsername("nobody");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────
    //  MR-02: findByEmail()
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MR-02 (findByEmail): 존재하는 email → Optional<Member> 값 있음")
    void findByEmail_exists_returnsOptionalWithMember() {
        Optional<Member> result = memberRepository.findByEmail("testuser@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("testuser@test.com");
    }

    // ─────────────────────────────────────────────────
    //  MR-03: existsByUsername()
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MR-03: existsByUsername() - 중복 username → true")
    void existsByUsername_duplicate_returnsTrue() {
        assertThat(memberRepository.existsByUsername("testuser")).isTrue();
    }

    @Test
    @DisplayName("MR-03: existsByUsername() - 없는 username → false")
    void existsByUsername_unique_returnsFalse() {
        assertThat(memberRepository.existsByUsername("newuser")).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  MR-04: existsByEmail()
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MR-04: existsByEmail() - 중복 email → true")
    void existsByEmail_duplicate_returnsTrue() {
        assertThat(memberRepository.existsByEmail("testuser@test.com")).isTrue();
    }

    @Test
    @DisplayName("MR-04: existsByEmail() - 없는 email → false")
    void existsByEmail_unique_returnsFalse() {
        assertThat(memberRepository.existsByEmail("new@test.com")).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  MR-05: existsByDisplayName()
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MR-05: existsByDisplayName() - 중복 displayName → true")
    void existsByDisplayName_duplicate_returnsTrue() {
        assertThat(memberRepository.existsByDisplayName("테스트닉")).isTrue();
    }

    @Test
    @DisplayName("MR-05: existsByDisplayName() - 없는 displayName → false")
    void existsByDisplayName_unique_returnsFalse() {
        assertThat(memberRepository.existsByDisplayName("새닉네임")).isFalse();
    }

    // ─────────────────────────────────────────────────
    //  MR-06: existsByUsernameAndIdNot() - 회원 수정 시 중복 체크
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("MR-06: existsByUsernameAndIdNot() - 본인 제외 중복 체크 (본인이면 false)")
    void existsByUsernameAndIdNot_selfExcluded_returnsFalse() {
        assertThat(
                memberRepository.existsByUsernameAndIdNot("testuser", savedMember.getId())
        ).isFalse();
    }

    @Test
    @DisplayName("MR-06: existsByUsernameAndIdNot() - 타인의 username과 중복 → true")
    void existsByUsernameAndIdNot_otherUser_returnsTrue() {
        Member another = memberRepository.save(Member.builder()
                .username("another")
                .password("pw")
                .email("another@test.com")
                .displayName("다른닉")
                .roles(Set.of("ROLE_USER"))
                .build());

        assertThat(
                memberRepository.existsByUsernameAndIdNot("testuser", another.getId())
        ).isTrue();
    }
}
