package suneido;

public class SuClass extends SuValue {
	
//	private HashMap<SuValue,SuValue> m;

	@Override
	public String toString() {
		return "a Suneido class";
	}

	public SuValue invoke(int method, SuValue ... args) {
		return invoke2(method, args);
	}
	public SuValue invoke2(int method, SuValue[] args) {
		if (method == SuSymbol.DEFAULTi)
			throw unknown_method(method);
		return invoke2(SuSymbol.DEFAULTi, args);
	}
	
	/**
	 * Implements Suneido's argument handling.
	 * Called at the start of generated sub-class methods.
	 *  
	 * @param nlocals	The number of local variables required, including parameters.
	 * @param args		The arguments as an SuValue array.<br />
	 * 					fn(... @args ...) => ... EACH, args ...<br />
	 * 					fn(... name: arg ...) => ... NAMED, name, arg ...<br />
	 * 					Unlike cSuneido, multiple EACH's are allowed.
	 * @param params	A variable number of parameter names as symbol indexes.<br />
	 * 					function (@args) => EACH, args<br />
	 * 					No other params are allowed with EACH.
	 * @return	The locals SuValue array initialized from args.
	 */
	public SuValue[] massage(int nlocals, final SuValue[] args, final int ... params) {
		SuValue[] locals = new SuValue[nlocals];
		if (args.length == 0)
			return locals;
		if (params.length > 0 && params[0] == SuSymbol.EACHi) {
			// function (@params)
			if (args[0] == SuSymbol.EACH && args.length == 2)
				// optimize function (@params) (@args)
				locals[0] = new SuContainer((SuContainer) args[1]);
			else {
				SuContainer c = new SuContainer();
				locals[0] = c;
				for (int i = 0; i < args.length; ++i) {
					if (args[i] == SuSymbol.NAMED) {
						c.putdata(args[i + 1], args[i + 2]);
						i += 2;
					}
					else if (args[i] == SuSymbol.EACH)
						c.merge((SuContainer) args[++i]);
					else
						c.vec.add(args[i]);
				}
			}
		} else {
			assert nlocals >= params.length;
			int li = 0;
			for (int i = 0; i < args.length; ++i) {
				if (args[i] == SuSymbol.NAMED) {
					for (int j = 0; j < params.length; ++j)
						if (SuSymbol.symbol(params[j]) == args[i + 1])
							locals[j] = args[i + 2];
					// else ignore named arg not matching param
					i += 2;
				}
				else if (args[i] == SuSymbol.EACH) {
					SuContainer c = (SuContainer) args[++i];
					if (c.vecsize() > nlocals - li)
						throw new SuException("too many arguments");
					for (SuValue x : c.vec)
						locals[li++] = x;
					for (int j = 0; j < params.length; ++j) {
						SuValue x = c.map.get(SuSymbol.symbol(params[j]));
						if (x != null)
							locals[j] = x;
					}
				}
				else
					locals[li++] = args[i];
			}
		}
		return locals;
	}

	//TODO handle @+# args, maybe just add EACH1 since we only ever use @+1
	//TODO check for missing arguments (but what about defaults?)
}
