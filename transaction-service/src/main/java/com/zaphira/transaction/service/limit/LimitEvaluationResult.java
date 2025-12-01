package com.zaphira.transaction.service.limit;

import com.zaphira.transaction.model.enums.AuthorizationMethod;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LimitEvaluationResult {

    private final boolean authorizationRequired;
    private final AuthorizationMethod method;
    private final String reason;
}


