package org.kernelab.txbg.ext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.Arbiter.ConditionInterpreter;

public class LessEqualConditionInterpreter implements ConditionInterpreter
{
	private Matcher	matcher	= Pattern.compile("<=([\\d\\D]+)").matcher("");

	public boolean identify(Object value, String condition)
	{
		return (value instanceof Number || value instanceof String) && matcher.reset(condition).matches();
	}

	public boolean judge(Object value, String condition)
	{
		boolean judge = false;

		if (matcher.reset(condition).matches()) {
			String s = value.toString();
			if (value instanceof Number) {
				judge = Double.valueOf(s).compareTo(Double.valueOf(matcher.group(1))) <= 0;
			} else {
				judge = s.compareTo(matcher.group(1)) <= 0;
			}
		}

		return judge;
	}
}
