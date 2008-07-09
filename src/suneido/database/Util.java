package suneido.database;

import java.util.Arrays;
import java.util.List;

/**
 * Miscellaneous functions.
 * 
 * @author Andrew McKinlay
 *         <p>
 *         <small>Copyright 2008 Suneido Software Corp. All rights reserved.
 *         Licensed under GPLv2.</small>
 *         </p>
 */
public class Util {

	public static String listToCommas(List<String> list) {
		if (list == null || list.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (String s : list)
			sb.append(s).append(",");
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	public static List<String> commasToList(String commas) {
		return Arrays.asList(commas.split(","));
	}

	public static int find(List<String> list, String value) {
		for (int i = 0; i < list.size(); ++i)
			if (list.get(i).equals(value))
				return i;
		return -1;
	}

}
