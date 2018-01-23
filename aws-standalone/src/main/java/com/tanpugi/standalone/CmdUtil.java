package com.tanpugi.standalone;

public class CmdUtil {

	public static String convertArg(String arg, String name,
		String defaultValue, ParamValidator validator) {
		String value = getValue(arg, name, validator);
		if (value != null) { return String.valueOf(value); }
		return defaultValue;
	}

	public static String[] convertArg(String arg, String name,
		String[] defaultValue, ParamValidator validator) {
		String value = getValue(arg, name, validator);
		if (value != null) { return value.split(","); }
		return defaultValue;
	}

	public static int convertArg(String arg, String name,
		int defaultValue, ParamValidator validator) {
		String value = getValue(arg, name, validator);
		if (value != null) { return Integer.valueOf(value); }
		return defaultValue;
	}

	public static boolean convertArg(String arg, String name,
		boolean defaultValue, ParamValidator validator) {
		String value = getValue(arg, name, validator);
		if (value != null) { return Boolean.valueOf(value); }
		return defaultValue;
	}
	
	private static String getValue(String arg, String name,
		ParamValidator validator) {
		name = name + "=";
		if (arg.contains(name)) {
			String[] keyval = arg.split(name);
			String val = keyval[1];
			if (validator != null) {
				validator.validate(val);
			}
			return val;
		}
		return null;
	}
}
