package com.zaphira.user.repository;

import com.zaphira.user.model.entities.PredefinedSecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredefinedSecurityQuestionRepository extends JpaRepository<PredefinedSecurityQuestion, Long> {


    List<PredefinedSecurityQuestion> findByActiveTrueOrderByDisplayOrder();

    // Questions non encore configurées par l'utilisateur
    @Query("SELECT psq FROM PredefinedSecurityQuestion psq " +
            "WHERE psq.active = true " +
            "AND psq.id NOT IN (" +
            "    SELECT usa.question.id FROM UserSecurityAnswer usa WHERE usa.user.userId = :userId" +
            ") " +
            "ORDER BY psq.displayOrder")
    List<PredefinedSecurityQuestion> findAvailableQuestionsForUser(@Param("userId") Long userId);

}
