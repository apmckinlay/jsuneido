/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.Suneido.dbpkg;
import static suneido.util.Util.commaJoiner;
import static suneido.util.Verify.verify;

import java.nio.ByteBuffer;
import java.util.*;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.server.DbmsTran;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.language.*;
import suneido.language.builtin.RecordMethods;
import suneido.language.builtin.SuTransaction;
import suneido.util.Util;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@NotThreadSafe
public abstract class SuRecordOld extends SuContainer {
	private Header hdr;
	private SuTransaction tran;
	private int recadr;
	enum Status { NEW, OLD, DELETED };
	private Status status;
	private final List<Object> observers = Lists.newArrayList();
	private final Set<Object> invalid = Sets.newHashSet(); // used by rules
	private final Map<Object, Set<Object>> dependencies = Maps.newHashMap();
	private final Deque<Object> activeRules = new ArrayDeque<Object>();
	private final Set<Object> invalidated = Sets.newLinkedHashSet(); // for observers
	private final Map<Object, Object> attachedRules = Maps.newHashMap();

	{ defval = ""; }

	public SuRecordOld() {
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
	}

	public SuRecordOld(SuRecordOld r) {
		super(r);
		hdr = null;
		tran = null;
		recadr = 0;
		status = r.status;
		for (Map.Entry<Object, Set<Object>> e : r.dependencies.entrySet())
			dependencies.put(e.getKey(), new HashSet<Object>(e.getValue()));
	}

	public SuRecordOld(Row row, Header hdr) {
		this(row, hdr, (SuTransaction) null);
	}

	public SuRecordOld(Row row, Header hdr, DbmsTran tran) {
		this(row, hdr, tran == null ? null : new SuTransaction(tran));
	}

	public SuRecordOld(Row row, Header hdr, SuTransaction tran) {
		this.hdr = hdr;
		this.tran = tran;
		this.recadr = row.address();
		status = Status.OLD;

		for (Iterator<Row.Entry> iter = row.iterator(hdr); iter.hasNext();) {
			Row.Entry e = iter.next();
			addField(e.field, e.value);
		}
	}

	public SuRecordOld(Record rec, List<String> flds, SuTransaction tran) {
		hdr = null;
		this.tran = tran;
		recadr = 0;
		status = Status.OLD;
		int i = 0;
		for (String field : flds)
			addField(field, rec.getRaw(i++));
	}

	@Override
	public void clear() {
		super.clear();
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
	public String toString() {
		return toString("[", "]");
	}

	@Override
	public void pack(ByteBuffer buf) {
		super.pack(buf, Pack.Tag.RECORD);
	}

	public static Object unpack(ByteBuffer buf) {
		return unpack(buf, new SuRecord());
	}

	@Override
	public void put(Object key, Object value) {
		invalid.remove(key); // before get
		if (containsKey(key)) {
			Object old = super.get(key);
			if (old != null && old.equals(value))
				return;
		}
		super.put(key, value);
		invalidateDependents(key);
		callObservers(key);
	}

	private void invalidateDependents(Object key) {
		if (dependencies.containsKey(key))
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
	public void invalidate(Object member) {
		assert invalidated.isEmpty();
		invalidate1(member);
		callObservers(member);
	}

	@Override
	public Object get(Object key) {
		RuleContext.Rule ar = RuleContext.top();
		if (ar != null && ar.rec == this)
			addDependency(ar.member, key);

		Object result = getIfPresent(key);
		if (result == null || invalid.contains(key)) {
			Object x = callRule(key);
			if (x != null)
				result = x;
			else if (result == null)
				result = defval;
		}
		return result;
	}

	private void addDependency(Object src, Object dst) {
		if (!dependencies.containsKey(dst))
			dependencies.put(dst, new HashSet<Object>());
		dependencies.get(dst).add(src);
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
					//System.out.println("callRule " + key + " => " + x);
					if (x != null)
						putMap(key, x);
					return x;
				} else
					throw new SuException("invalid Rule_" + key);
			} finally {
				RuleContext.pop(this, key);
			}
		} finally {
			assert activeRules.peek() == key;
			activeRules.pop();
		}
	}

	@Override
	public Record toDbRecord(Header hdr) {
		List<String> fldsyms = hdr.output_fldsyms();
		Map<Object, Set<Object>> deps = getDeps(hdr, fldsyms);
		// PERF don't add trailing empty fields

		RecordBuilder rb = dbpkg.recordBuilder();
		Object x;
		String ts = hdr.timestamp_field();
		for (String f : fldsyms)
			if (f == null)
				rb.addMin();
			else if (f.equals(ts))
				rb.add(TheDbms.dbms().timestamp());
			else if (deps.containsKey(f))
				rb.add(commaJoiner.join(deps.get(f)));
			else if (null != (x = get(f)))
				rb.add(x);
			else
				rb.addMin();
		return rb.build();
	}

	private Map<Object, Set<Object>> getDeps(Header hdr, List<String> fldsyms) {
		// access all fields to ensure dependencies are created
		for (String f : hdr.output_fldsyms())
			if (!f.equals("-"))
				get(f);
		// invert dependencies
		Map<Object, Set<Object>> deps = new HashMap<Object, Set<Object>>();
		for (Map.Entry<Object, Set<Object>> e : dependencies.entrySet())
			for (Object x : e.getValue()) {
				String d = x + "_deps";
				if (!fldsyms.contains(d))
					continue;
				if (!deps.containsKey(d))
					deps.put(d, new HashSet<Object>());
				deps.get(d).add(e.getKey());
			}
		return deps;
	}

	public void update() {
		ck_modify("Update");
		Record newrec = toDbRecord(hdr);
		recadr = tran.getTransaction().update(recadr, newrec);
		verify(recadr != 0);
	}

	public void delete() {
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

	public boolean isNew() {
		return status == Status.NEW;
	}

	public SuTransaction getTransaction() {
		return tran;
	}

	public void addObserver(Object observer) {
		observers.add(observer);
	}

	public void removeObserver(Object observer) {
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
			new ThreadLocal<List<ActiveObserver>>() {
				@Override
				public List<ActiveObserver> initialValue() {
					return new ArrayList<ActiveObserver>();
				}
			};

	/**
	 * Calls observers for the specified field,
	 * and then for any other invalidated fields.
	 */
	public void callObservers(Object member) {
		callObservers2(member);
		invalidated.remove(member);
		// can't iterate normally because of potential concurrent modification
		while (! invalidated.isEmpty()) {
			Iterator<Object> iter = invalidated.iterator();
			Object m = iter.next();
			iter.remove();
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

	public String getdeps(String field) {
		StringBuilder deps = new StringBuilder();
		for (Map.Entry<Object, Set<Object>> e : dependencies.entrySet())
			for (Object x : e.getValue())
				if (field.equals(x))
					deps.append(",").append(e.getKey().toString());
		return deps.length() == 0 ? "" : deps.substring(1);
	}

	public void setdeps(String field, String deps) {
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

	public void attachRule(String field, Object rule) {
		attachedRules.put(field, rule);
	}

}
