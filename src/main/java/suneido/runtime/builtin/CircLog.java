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
			String str = "";
			for(int i = (qi + 1) % QSIZE; i != qi; i = (i + 1) % QSIZE )
				if (queue[i] != "" && queue[i] != null)
					str += queue[i] + "\n";
			return str;
		}
		queue[qi] = Ops.toStr(s).trim();
		qi = (qi + 1) % QSIZE;
		return null;
	}
}
