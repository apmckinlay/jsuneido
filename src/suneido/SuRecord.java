/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.database.immudb.Table.isSpecialField;
import static suneido.util.Util.commaJoiner;
import static suneido.util.Verify.verify;

import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.base.Objects;
import com.google.common.collect.*;

import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.server.DbmsTran;
import suneido.runtime.*;
import suneido.runtime.builtin.RecordMethods;
import suneido.runtime.builtin.SuTransaction;
import suneido.util.CommaStringBuilder;
import suneido.util.NotThreadSafe;
import suneido.util.Util;

@NotThreadSafe
public class SuRecord extends SuObject {
	private Header hdr;
	private SuTransaction tran;
	private int recadr;
	enum Status { NEW, OLD, DELETED }

	private Status status;
	private final List<Object> observers = Lists.newArrayList();
	private final Set<Object> invalid; // used by rules
	private final SetMultimap<Object, Object> dependencies;
	private final Deque<Object> activeRules = new ArrayDeque<>();
	private final Set<Object> invalidated = Sets.newLinkedHashSet(); // for observers
	private final Map<Object, Object> attachedRules = Maps.newHashMap();

	{ defval = ""; }

	public SuRecord() {
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
		invalid = Sets.newHashSet();
		dependencies = HashMultimap.create();
	}

	public SuRecord(SuRecord r) {
		super(r);
		hdr = null;
		tran = null;
		recadr = 0;
		status = r.status;
		invalid = Sets.newHashSet(r.invalid);
		dependencies = HashMultimap.create(r.dependencies);
	}

	public SuRecord(Row row, Header hdr) {
		this(row, hdr, (SuTransaction) null);
	}

	public SuRecord(Row row, Header hdr, DbmsTran tran) {
		this(row, hdr, tran == null ? null : new SuTransaction(tran));
	}

	public SuRecord(Row row, Header hdr, SuTransaction tran) {
		this.hdr = hdr;
		this.tran = tran;
		this.recadr = row.address();
		status = Status.OLD;
		invalid = Sets.newHashSet();
		dependencies = HashMultimap.create();

		for (Iterator<Row.Entry> iter = row.iterator(hdr); iter.hasNext();) {
			Row.Entry e = iter.next();
			if (! isSpecialField(e.field))
				addField(e.field, e.value);
		}
	}

	public SuRecord(Record rec, List<String> flds, SuTransaction tran) {
		hdr = null;
		this.tran = tran;
		recadr = 0;
		status = Status.OLD;
		invalid = Sets.newHashSet();
		dependencies = HashMultimap.create();
		int i = 0;
		for (String field : flds)
			addField(field, rec.getRaw(i++));
	}

	public synchronized void clear() {
		super.deleteAll();
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
	}

	private void addField(String field, ByteBuffer buf) {
		if (field.equals("-") || buf.remaining() == 0)
			return;
		Object x = Pack.unpack(buf);
		if (field.endsWith("_deps"))
			dependencies(baseFieldName(field), (String) x);
		else
			super.put(field, x);
	}

	/** remove "_deps" suffix */
	private static String baseFieldName(String field) {
		return field.substring(0, field.length() - 5);
	}

	private void dependencies(String mem, String s) {
		for (int pos = 0; pos < s.length();) {
			int end = s.indexOf(',', pos);
			if (end == -1)
				end = s.length();
			String t = s.substring(pos, end);
			addDependency(mem, t);
			pos = end + 1;
		}
	}

	@Override
	public void rString(StringBuilder sb, InProgress inProgress) {
		toString(sb, "[", "]", inProgress);
	}

	@Override
	public String show() {
		return show("[", "]");
	}

	@Override
	public synchronized void pack(ByteBuffer buf) {
		super.pack(buf, Pack.Tag.RECORD);
	}

	public static Object unpack(ByteBuffer buf) {
		return unpack(buf, new SuRecord());
	}

	@Override
	public synchronized void put(Object key, Object value) {
		invalid.remove(key); // before get
		Object old = containsKey(key) ? super.get(key) : null;
		super.put(key, value);
		if (old != null && old.equals(value))
			return;
		invalidateDependents(key);
		callObservers(key);
	}

	@Override
	public synchronized boolean delete(Object key) {
		boolean result = super.delete(key);
		if (result) {
			invalidateDependents(key);
			callObservers(key);
		}
		return result;
	}

	@Override
	public synchronized boolean erase(Object key) {
		boolean result = super.erase(key);
		if (result) {
			invalidateDependents(key);
			callObservers(key);
		}
		return result;
	}

	private void invalidateDependents(Object key) {
		for (Object dep : dependencies.get(key))
			invalidate1(dep);
	}

	private void invalidate1(Object member) {
		if (invalid.contains(member))
			return;
		invalidated.add(member); // for observers
		invalid.add(member);
		invalidateDependents(member);
	}

	/** called by Suneido record.Invalidate */
	public synchronized void invalidate(Object member) {
		invalidate1(member);
		callObservers(member);
	}

	@Override
	public Object get(Object key) {
		return getDef(key, defval);
	}

	public Object getDef(Object key, Object def) {
		Object result;
		synchronized (this) {
			RuleContext.Rule ar = RuleContext.top();
			if (ar != null && ar.rec == this && !ar.member.equals(key))
				addDependency(ar.member, key);

			result = getIfPresent(key);
			if (result != null && !invalid.contains(key))
				return result;
			Object x = getIfSpecial(key);
			if (x != null)
				return x;
		}
		Object x = callRule(key);
		synchronized (this) {
			if (x != null)
				result = x;
			else if (result == null)
				result = defaultValue(key, def);
			return result;
		}
	}

	@Override
	protected SuObject dup() {
		return new SuRecord(this);
	}

	private Object getIfSpecial(Object key) {
		if (key instanceof String && isSpecialField((String) key)) {
			String base = Util.beforeLast((String) key, "_");
			Object x = getIfPresent(base);
			if (x != null)
				return (x instanceof String)
						? ((String) x).toLowerCase()
						: x; // no transform if not string
		}
		return null;
	}

	private void addDependency(Object src, Object dst) {
		dependencies.put(dst, src);
	}

	private Object callRule(Object k) {
		invalid.remove(k);
		if (! Ops.isString(k))
			return null;
		String key = Ops.toStr(k);
		Object rule = attachedRules.get(key);
		if (rule == null && defval != null)
			rule = Suneido.context.tryget("Rule_" + key);
		if (rule == null)
			return null;
		// prevent cycles
		if (activeRules.contains(key))
			return null;
		activeRules.push(key);
		try {
			RuleContext.push(this, key);
			try {
				if (rule instanceof SuValue) {
					Object x = ((SuValue) rule).eval(this);
					if (x != null && ! getReadonly())
						putMap(key, x);
					return x;
				} else
					throw new SuException("invalid Rule_" + key);
			} catch (Throwable e) {
				throw new SuException(e + " (rule for " + key + ")", e, true);
			} finally {
				RuleContext.pop(this, key);
			}
		} finally {
			assert activeRules.peek() == key;
			activeRules.pop();
		}
	}

	@Override
	public synchronized Record toDbRecord(Header hdr) {
		List<String> fldsyms = hdr.output_fldsyms();
		Map<Object, Set<Object>> deps = getDeps(hdr, fldsyms);
		// PERF don't add trailing empty fields

		RecordBuilder rb = new RecordBuilder();
		Object x;
		String ts = hdr.timestamp_field();
		Object tsval = null;
		for (String f : fldsyms)
			if (f == null)
				rb.addMin();
			else if (f.equals(ts))
				rb.add(tsval = TheDbms.dbms().timestamp());
			else if (deps.containsKey(f))
				rb.add(commaJoiner.join(deps.get(f)));
			else if (null != (x = get(f)))
				rb.add(x);
			else
				rb.addMin();
		if (tsval != null && ! getReadonly())
			super.put(ts, tsval);
		return rb.build();
	}

	private Map<Object, Set<Object>> getDeps(Header hdr, List<String> fldsyms) {
		// access all fields to ensure dependencies are created
		for (String f : hdr.output_fldsyms())
			if (!f.equals("-"))
				get(f);
		// invert dependencies
		Map<Object, Set<Object>> deps = new HashMap<>();
		for (Object key : dependencies.keySet())
			for (Object x : dependencies.get(key)) {
				String d = x + "_deps";
				if (!fldsyms.contains(d))
					continue;
				if (! deps.containsKey(d))
					deps.put(d, new HashSet<>());
				deps.get(d).add(key);
			}
		return deps;
	}

	public synchronized void update(SuObject ob) {
		ck_modify("Update");
		Record newrec = ob.toDbRecord(hdr);
		recadr = tran.getTransaction().update(recadr, newrec);
		verify(recadr != 0);
	}

	public synchronized void delete() {
		ck_modify("Delete");
		tran.getTransaction().erase(recadr);
	}

	private void ck_modify(String op) {
		if (tran == null)
			throw new SuException("record." + op + ": no Transaction");
		if (tran.isEnded())
			throw new SuException("record." + op
					+ ": Transaction already completed");
		if (status != Status.OLD)
			throw new SuException("record." + op + ": not an old record");
		if (recadr == 0)
			throw new SuException("record." + op + ": not a database record");
	}

	@Override
	public String typeName() {
		return "Record";
	}

	public synchronized boolean isNew() {
		return status == Status.NEW;
	}

	public synchronized SuTransaction getTransaction() {
		return tran;
	}

	public synchronized void addObserver(Object observer) {
		observers.add(observer);
	}

	public synchronized void removeObserver(Object observer) {
		observers.remove(observer);
	}

	private static class ActiveObserver {
		public Object observer;
		public Object member;

		public ActiveObserver(Object observer, Object member) {
			this.observer = observer;
			this.member = member;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof ActiveObserver))
				return false;
			ActiveObserver that = (ActiveObserver) other;
			return Objects.equal(observer, that.observer) &&
					Objects.equal(member, that.member);
		}

		@Override
		public int hashCode() {
			throw new UnsupportedOperationException();
		}
	}

	public static final ThreadLocal<List<ActiveObserver>> activeObservers =
			ThreadLocal.withInitial(ArrayList::new);

	/**
	 * Calls observers for the specified field,
	 * and then for any other invalidated fields.
	 */
	public synchronized void callObservers(Object member) {
		callObservers2(member);
		// can't iterate normally because of potential concurrent modification
		while (! invalidated.isEmpty()) {
			Iterator<Object> iter = invalidated.iterator();
			Object m = iter.next();
			iter.remove();
			if (! m.equals(member))
				callObservers2(m);
		}
	}

	/** Call all the observers for a particular field */
	private void callObservers2(Object member) {
		List<ActiveObserver> aos = activeObservers.get();
		for (Object observer : observers) {
			ActiveObserver ao = new ActiveObserver(observer, member);
			if (aos.contains(ao))
				continue;
			aos.add(ao);
			try {
				if (observer instanceof SuBoundMethod)
					((SuBoundMethod) observer).call(Args.Special.NAMED, "member",
							member);
				else if (observer instanceof SuValue)
					((SuValue) observer).eval(this, Args.Special.NAMED,
							"member", member);
				else
					throw new SuException("invalid observer");
			} finally {
				aos.remove(ao);
			}
		}
	}

	public synchronized String getdeps(String field) {
		CommaStringBuilder deps = new CommaStringBuilder();
		for (Object key : dependencies.keySet())
			if (dependencies.get(key).contains(field))
				deps.add(key);
		return deps.toString();
	}

	public synchronized void setdeps(String field, String deps) {
		for (String d : Util.commaSplitter(deps))
			addDependency(field, d);
	}

	@Override
	public SuValue lookup(String method) {
		SuValue m = RecordMethods.methods.getMethod(method);
		if (m != null)
			return m;
		return super.lookup(method);
	}

	public synchronized void attachRule(String field, Object rule) {
		attachedRules.put(field, rule);
	}

}
