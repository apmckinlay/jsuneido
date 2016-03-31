/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.nio.ByteBuffer;

import suneido.SuContainer;
import suneido.SuValue;

/** Wraps an SuValue and delegates to it with synchronized methods */
public class ConcurrentValue extends SuValue {
	private final SuValue value;

	public ConcurrentValue(SuValue value) {
		this.value = value;
	}

	@Override
	synchronized public String toString() {
		return value.toString();
	}

	@Override
	synchronized public int hashCodeContrib() {
		return value.hashCodeContrib();
	}

	@Override
	synchronized public Object call(Object... args) {
		return value.call(args);
	}

	@Override
	synchronized public Object eval(Object self, Object... args) {
		return value.eval(self, args);

	}

	@Override
	synchronized public String typeName() {
		return value.typeName();
	}

	@Override
	synchronized public String valueName() {
		return value.valueName();
	}

	@Override
	synchronized public Object get(Object member) {
		return value.get(member);
	}

	@Override
	synchronized public void put(Object mem, Object val) {
		value.put(mem, val);
	}

	@Override
	synchronized public int packSize() {
		return value.packSize();
	}

	@Override
	synchronized public int packSize(int nest) {
		return value.packSize(nest);
	}

	@Override
	synchronized public void pack(ByteBuffer buf) {
		value.pack(buf);
	}

	@Override
	synchronized public SuContainer toContainer() {
		return value.toContainer();
	}

	@Override
	synchronized public Object call0() {
		return value.call0();
	}

	@Override
	synchronized public Object call1(Object a) {
		return value.call1(a);
	}

	@Override
	synchronized public Object call2(Object a, Object b) {
		return value.call2(a, b);
	}

	@Override
	synchronized public Object call3(Object a, Object b, Object c) {
		return value.call3(a, b, c);
	}

	@Override
	synchronized public Object call4(Object a, Object b, Object c, Object d) {
		return value.call4(a, b, c, d);
	}

	@Override
	synchronized public Object eval0(Object self) {
		return value.eval0(self);
	}

	@Override
	synchronized public Object eval1(Object self, Object a) {
		return value.eval1(self, a);
	}

	@Override
	synchronized public Object eval2(Object self, Object a, Object b) {
		return value.eval2(self, a, b);
	}

	@Override
	synchronized public Object eval3(Object self, Object a, Object b, Object c) {
		return value.eval3(self, a, b, c);
	}

	@Override
	synchronized public Object eval4(Object self, Object a, Object b, Object c, Object d) {
		return value.eval4(self, a, b, c, d);
	}

	@Override
	synchronized public SuValue lookup(String method) {
		return value.lookup(method);
	}

	@Override
	synchronized public String display() {
		return value.display();
	}

}
