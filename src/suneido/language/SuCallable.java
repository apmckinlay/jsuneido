package suneido.language;

import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	protected FunctionSpec params;
	protected Object[] constants;
	/** used to do super calls by methods and blocks within methods
	 *  set by {@link SuClass.linkMethods} */
	protected SuClass myClass;

	@Override
	public boolean isCallable() {
		return true;
	}

	public Object superInvokeN(Object self, String member) {
		return myClass.superInvoke(self, member);
	}
	public Object superInvokeN(Object self, String member, Object a) {
		return myClass.superInvoke(self, member, a);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b) {
		return myClass.superInvoke(self, member, a, b);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c) {
		return myClass.superInvoke(self, member, a, b, c);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d) {
		return myClass.superInvoke(self, member, a, b, c, d);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e) {
		return myClass.superInvoke(self, member, a, b, c, d, e);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y, Object z) {
		return myClass.superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z);
	}

}
