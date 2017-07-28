/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.regex.Pattern;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class RegexMatcher extends BaseMatcher<String> {
	String regex;
	Pattern pattern;

	RegexMatcher(String regex) {
		this.regex = regex;
		pattern = Pattern.compile(regex);
	}

	@Override
	public boolean matches(Object item) {
		return item instanceof String &&
				pattern.matcher((String) item).matches();
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("match " + regex);
	}

	public static Matcher<String> matches(String regex){
		return new RegexMatcher(regex);
	}

}

