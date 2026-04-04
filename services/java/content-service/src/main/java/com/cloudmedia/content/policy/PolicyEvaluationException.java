package com.cloudmedia.content.policy;

public class PolicyEvaluationException extends RuntimeException {

	public PolicyEvaluationException(String message) {
		super(message);
	}

	public PolicyEvaluationException(String message, Throwable cause) {
		super(message, cause);
	}
}
