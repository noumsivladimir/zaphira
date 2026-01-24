package com.zaphira.service_user.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecurityQuestionResponse {
	private Long id;
	private String question;

}
