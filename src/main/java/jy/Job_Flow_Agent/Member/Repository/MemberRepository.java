package jy.Job_Flow_Agent.Member.Repository;

import jy.Job_Flow_Agent.Member.Entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);

    Optional<Member> findByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByDisplayNameAndIdNot(String displayName, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsername(String username);

    boolean existsByDisplayName(String displayName);

    boolean existsByEmail(String email);
}
