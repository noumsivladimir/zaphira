package com.zaphira.auth.repository;

import com.zaphira.auth.model.Token;
import com.zaphira.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    List<Token> findAllByUserAndExpiredFalseAndRevokedFalse(User user);

    Optional<Token> findByToken(String token);
}
