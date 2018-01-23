package com.tanpugi.standalone;

@SuppressWarnings("serial")
public class ParamValidationException extends RuntimeException {

	public ParamValidationException() {
		super();
	}
	public ParamValidationException(String msg) {
		super(msg);
	}
}
