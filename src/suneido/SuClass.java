package suneido;

import java.util.HashMap;
import java.util.Map;

public class SuClass extends SuValue {
	
	private HashMap<SuValue,SuValue> m;

	@Override
	public String toString() {
		return "a Suneido class";
	}

	public SuValue invoke(int method, SuValue ... args) {
		return invoke2(method, args);
	}
	public SuValue invoke2(int method, SuValue[] args) {
		if (method == SuSymbol.DEFAULT)
			throw unknown_method(method);
		return invoke2(SuSymbol.DEFAULT, args);
	}
	
	public SuValue[] massage(int nlocals, SuValue[] args, int ... params) {
		boolean paramsEach = params[params.length - 1] == SuSymbol.EACH;
		int nparams = params.length - (paramsEach ? 1 : 0);
		assert nlocals >= nparams;
		SuValue[] locals = new SuValue[nlocals];
		if (paramsEach)
			locals[0] = new SuContainer();
		int i = 0;
//		int li = 0;
//		for (; i < args.length; ++i)
//			if (args[i] == SuSymbol.NAMED) {
//				if (paramsEach)
//					locals[0].putdata(args[i + 1], args[i + 2]);
//				else
//					for (int j = 0; j < nparams; ++j)
//						if (SuSymbol.symbols(params[j]) == args[i + 1])
//							locals[j] = args[i + 2];
//					// else ignore named arg not matching param
//				i += 2;
//			}
//			else if (args[i] != SuSymbol.EACH) {
//				SuContainer c = (SuContainer) args[i + 2];
//				for (SuValue x : c.vec)
//					if (li < nparams)
//						locals[li++] = x;
//					else
//						throw new SuException("too many arguments");
//				
//					
//				i += 2;
//			}
//			else if (paramsEach)
//				locals[0].add(args[i]);
//			else
//				locals[li++] = args[i];
		if (i == args.length - 1)
			return locals;
		
		// NAMED or EACH
		
		
		return locals;
	}

	//TODO massage arguments
	//TODO data members
}
