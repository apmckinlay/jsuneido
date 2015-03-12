package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class CircLog { 
	static final int QSIZE = 500;
	static String [] queue = new String [QSIZE];
	static int qi = 0;
	
	@Params("string = false")
	public static Object CircLog(Object s) {
		if (s == Boolean.FALSE) {
			StringBuilder sb = new StringBuilder();
			for(int i = (qi + 1) % QSIZE; i != qi; i = (i + 1) % QSIZE )
				if (queue[i] != "" && queue[i] != null) {
					sb.append(queue[i]);
					sb.append("\n");
				}
			return sb.toString();
		}
		String str = Ops.toStr(s).trim();
		if (str.isEmpty())
			return null;
		queue[qi] = str;
		qi = (qi + 1) % QSIZE;
		return null;
	}
}
