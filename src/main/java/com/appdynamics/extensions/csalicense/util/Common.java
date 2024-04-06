package com.appdynamics.extensions.csalicense.util;

import java.util.List;

public class Common {

	public static String getLogHeader(Object classObject, String method) {
		return String.format("[%s:%s]", classObject.getClass().getSimpleName(), method);
	}

	public static String getCookies(List<String> cookies) {

		StringBuffer returnCookies = new StringBuffer();

		for (int index = 0; index < cookies.size(); index++) {
			returnCookies.append(cookies.get(index) + ";");
		}

		return returnCookies.toString();
	}

}
