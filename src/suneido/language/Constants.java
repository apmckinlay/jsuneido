package suneido.language;

import java.util.HashMap;
import java.util.Map;

import suneido.SuValue;

/**
 * Stores an array of constants for each generated class.
 * {@link CompileGenerator} calls put.
 *
 * @author Andrew McKinlay
 */
public class Constants {
	private static Map<String, SuValue[]> constants =
			new HashMap<String, SuValue[]>();

	public static void put(String name, SuValue[] values) {
		constants.put(name, values);
	}

	public static SuValue[] get(String name) {
		return constants.get(name);
	}
}
