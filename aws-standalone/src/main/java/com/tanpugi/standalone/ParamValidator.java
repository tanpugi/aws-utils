package com.tanpugi.standalone;

public class ParamValidator {

	private boolean isRequired;
	
	public static final ParamValidator IS_REQUIRED = new ParamValidator("REQ");

	public ParamValidator(String pattern) {
		String[] pats = pattern.split(" ");
		for (String pat : pats) {
			if (pat.contains("REQ")) { isRequired = true; }
		}
	}
	
	public void validate(String o) {
		validateRequired(isRequired, o); 
	}
	
	private void validateRequired(boolean isRequired, String o) {
		if (!isRequired) { return; }
		if (o == null) { 
			throw new ParamValidationException("Required"); 
		}
	}
}
