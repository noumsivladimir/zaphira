package com.zaphira.user.repository;

import com.zaphira.user.model.entities.User;
import com.zaphira.user.model.entities.UserSecurityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSecurityAnswerRepository extends JpaRepository<UserSecurityAnswer, Long> {

    @Query("SELECT usa FROM UserSecurityAnswer usa WHERE usa.user.userId = :userId")
    List<UserSecurityAnswer> findByUserQuestionId(@Param("userId") Long userId);


    @Query("SELECT COUNT(usa) FROM UserSecurityAnswer usa WHERE usa.user.userId = :userId")
    int numberOfAnswers(@Param("userId") Long userId);


    @Query("SELECT CASE WHEN COUNT(usa) > 0 THEN true ELSE false END FROM UserSecurityAnswer usa WHERE usa.user.userId = :userId AND usa.question.id = :questionId")
    boolean existsByUser_IdAndQuestion_Id(@Param("userId") Long userId, @Param("questionId") Long questionId);


    @Modifying
    @Query("DELETE FROM UserSecurityAnswer usa WHERE usa.user.userId = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    @Query("SELECT usa FROM UserSecurityAnswer usa " +
            "JOIN FETCH usa.question q " +
            "WHERE usa.user.userId = :userId")
     List<UserSecurityAnswer> findByUserIdWithQuestion(@Param("userId") Long userId);

//    @Query("""
//    SELECT usa
//    FROM UserSecurityAnswer usa
//    JOIN FETCH usa.question q
//    WHERE usa.user.userId = :userId
//""")
//    List<UserSecurityAnswer> findByUserdId(@Param("userId") Long userId);



    Long user(User user);
}
