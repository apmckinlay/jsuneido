package suneido;

//import java.util.HashMap;

/**
 * The Java base class for Suneido classes.
 * The Java class hierarchy is "flat".
 * All compiled Suneido classes derive directly from SuClass.
 * Suneido inheritance is handled by invoke.
 * A Suneido class with "no" parent calls super.invoke from its invoke's default
 * else it calls Globals.get(parent).invoke2
 */
public class SuClass extends SuValue {
//	private HashMap<SuValue,SuValue> m;

	@Override
	public String toString() {
		return "a Suneido class";
	}

	@Override
	public SuValue invoke(SuValue self, int method, SuValue ... args) {
		if (method == Symbols.CALLi)
			method = Symbols.CALL_CLASS;
		else if (method == Symbols.CALL_INSTANCE)
			method = Symbols.CALLi;
		// CALLi => create instance (if class doesn't implement CallClass)
		if (method != Symbols.DEFAULTi) {
			// if method not found
			// add method to beginning of args and call Default
			SuValue newargs[] = new SuValue[1 + args.length];
			System.arraycopy(args, 0, newargs, 1, args.length);
			newargs[0] = Symbols.symbol(method);
			return invoke(self, Symbols.DEFAULTi, newargs);
		} else
			throw unknown_method(args[0]);
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
	public static SuValue[] massage(int nlocals, final SuValue[] args, final int ... params) {
		boolean params_each = params.length > 0 && params[0] == Symbols.EACHi;
		
		// "fast" path - when possible, avoid alloc and just return args
		if (nlocals == args.length && ! params_each)
			for (int i = 0; ; ++i)
				if (i >= args.length)
					return args;
				else if (args[i] == Symbols.EACH || args[i] != Symbols.NAMED)
					break ;

		// "slow" path - alloc and copy into locals
		SuValue[] locals = new SuValue[nlocals];
		if (args.length == 0)
			return locals;
		if (params_each) {
			// function (@params)
			if (args[0] == Symbols.EACH && args.length == 2)
				// optimize function (@params) (@args)
				locals[0] = new SuContainer((SuContainer) args[1]);
			else {
				SuContainer c = new SuContainer();
				locals[0] = c;
				for (int i = 0; i < args.length; ++i) {
					if (args[i] == Symbols.NAMED) {
						c.putdata(args[i + 1], args[i + 2]);
						i += 2;
					}
					else if (args[i] == Symbols.EACH)
						c.merge((SuContainer) args[++i]);
					else
						c.vec.add(args[i]);
				}
			}
		} else {
			assert nlocals >= params.length;
			int li = 0;
			for (int i = 0; i < args.length; ++i) {
				if (args[i] == Symbols.NAMED) {
					for (int j = 0; j < params.length; ++j)
						if (Symbols.symbol(params[j]) == args[i + 1])
							locals[j] = args[i + 2];
					// else ignore named arg not matching param
					i += 2;
				}
				else if (args[i] == Symbols.EACH || args[i] == Symbols.EACH1) {
					int start = args[i] == Symbols.EACH ? 0 : 1;
					SuContainer c = (SuContainer) args[++i];
					if (c.vecsize() - start > nlocals - li)
						throw new SuException("too many arguments");
					for (int j = start; j < c.vecsize(); ++j)
						locals[li++] = c.vec.get(j);
					for (int j = 0; j < params.length; ++j) {
						SuValue x = c.map.get(Symbols.symbol(params[j]));
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
	public static SuValue[] massage(final SuValue[] args, final int ... params) {
		return massage(params.length, args, params);
	}

	//TODO handle @+# args, maybe just add EACH1 since we only ever use @+1
	//TODO check for missing arguments (but what about defaults?)
}
