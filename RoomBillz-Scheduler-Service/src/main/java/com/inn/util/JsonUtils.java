package com.inn.util;


public class JsonUtils {
	
	public static String safeTruncate(String s, int max) {
		if (s == null)
			return null;
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}
}
