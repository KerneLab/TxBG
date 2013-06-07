package org.kernelab.txbg.ext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kernelab.basis.Arbiter.ConditionInterpreter;

public class RegexConditionInterpreter implements ConditionInterpreter
{
	private Matcher	matcher	= Pattern.compile("=~\\/(.*)\\/([gim]*)").matcher("");

	public boolean identify(Object value, String condition)
	{
		return matcher.reset(condition).matches();
	}

	public boolean judge(Object value, String condition)
	{
		boolean judge = false;

		if (matcher.reset(condition).matches()) {
			String expr = matcher.group(1);
			String flag = matcher.group(2);
			int flags = 0;
			if (flag.contains("i")) {
				flags |= Pattern.CASE_INSENSITIVE;
			}
			if (flag.contains("m")) {
				flags |= Pattern.MULTILINE;
			}
			judge = Pattern.compile(expr, flags).matcher(value.toString()).find();
		}

		return judge;
	}

}
