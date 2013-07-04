package suneido.language.jsdi.type;

import java.util.HashMap;

import suneido.SuException;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.language.Contexts;

/**
 * Trivial implementation of Context for testing purposes.
 * @author Victor Schappert
 * @since 20130703
 */
public final class SimpleContext extends ContextLayered {

	//
	// DATA
	//

	private final HashMap<String, String> objects;

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a simple context.
	 * @param contexts Contexts
	 * @param objects Array containing an even number of elements in which, for
	 * every pair <code>&lt;objects<sub>i</sub>,&nbsp;objects<sub>i+1</sub>&gt;</code>,
	 * <code>objects<sub>i</sub></code> contains the name of the object, and
	 * <code>objects<sub>i+1</sub></code> contains the code block for that
	 * object.
	 */
	SimpleContext(String[] objects) {
		super(new Contexts());
		this.objects = new HashMap<String, String>();
		final int N = objects.length;
		for (int k = 0; k < N;) {
			this.objects.put(objects[k++], objects[k++]);
		}
	}

	//
	// ANCESTOR CLASS: Context
	//

	@Override
	protected Object fetch(String name) {
		String src = objects.get(name);
		Object result = null;
		try {
			result = Compiler.compile(name, src);
			set(name, result);
		} catch (Exception e) {
			throw new SuException("error loading " + name, e);
		}
		return result;
	}
}
