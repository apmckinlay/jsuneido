/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.jsdi.DllInterface;
import suneido.jsdi.Factory;
import suneido.jsdi.JSDI;
import suneido.jsdi.StorageType;
import suneido.jsdi.type.BasicType;
import suneido.jsdi.type.InString;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.VoidType;
import suneido.util.testing.Assumption;
import static suneido.jsdi.abi.amd64.ParamRegisterType.*;

/**
 * Test for {@link ParamRegisterType}.
 *
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
public class ParamRegisterTypeTest {

	private static Factory factory;
	private static Type floatValueType;
	private static Type doubleValueType;
	private static final Type inStringType = InString.INSTANCE;
	private static final Type voidType = VoidType.INSTANCE;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIsOnWindows();
		factory = JSDI.getInstance().getFactory();
		floatValueType = factory.makeBasicType(BasicType.FLOAT,
				StorageType.VALUE, 1);
		doubleValueType = factory.makeBasicType(BasicType.DOUBLE,
				StorageType.VALUE, 1);
	}

	@Test
	public void testBasicFpTypes() {
		assertEquals(FLOAT, ParamRegisterType.fromParamType(floatValueType));
		assertEquals(DOUBLE, ParamRegisterType.fromParamType(doubleValueType));
	}

	@Test
	public void testBasicUINT64Types() {
		for (final BasicType bt : BasicType.values()) {
			for (final StorageType st : StorageType.values()) {
				if (StorageType.POINTER == st) {
					continue; // Pointer to basic type not allowed
				}
				Type type = factory.makeBasicType(bt, st, 1);
				if (StorageType.ARRAY == st) {
					assertEquals(UINT64, ParamRegisterType.fromParamType(type));
					for (int k = 2; k < 5; ++k) {
						type = factory.makeBasicType(bt, st, k);
						assertEquals(UINT64,
								ParamRegisterType.fromParamType(type));
					}
				} else if (StorageType.VALUE != st
						|| (BasicType.FLOAT != bt && BasicType.DOUBLE != bt)) {
					assertEquals(UINT64, ParamRegisterType.fromParamType(type));
				}
			}
		}
	}

	@Test
	public void testPack0Params() {
		assertEquals(0, ParamRegisterType.packFromParamTypes(voidType,
				voidType, voidType, voidType));
	}

	@Test
	public void testPack1Params() {
		assertEquals(0, ParamRegisterType.packFromParamTypes(inStringType,
				voidType, voidType, voidType));
		assertEquals(0x01000000, ParamRegisterType.packFromParamTypes(
				doubleValueType, voidType, voidType, voidType));
		assertEquals(0x02000000, ParamRegisterType.packFromParamTypes(
				floatValueType, voidType, voidType, voidType));
	}

	@Test
	public void testPack2Params() {
		assertEquals(0, ParamRegisterType.packFromParamTypes(inStringType,
				inStringType, voidType, voidType));
		assertEquals(0x01000000, ParamRegisterType.packFromParamTypes(
				doubleValueType, inStringType, voidType, voidType));
		assertEquals(0x02000000, ParamRegisterType.packFromParamTypes(
				floatValueType, inStringType, voidType, voidType));
		assertEquals(0x01020000, ParamRegisterType.packFromParamTypes(
				doubleValueType, floatValueType, voidType, voidType));
		assertEquals(0x02010000, ParamRegisterType.packFromParamTypes(
				floatValueType, doubleValueType, voidType, voidType));
	}

	@Test
	public void testPack3Params() {
		assertEquals(0, ParamRegisterType.packFromParamTypes(inStringType,
				inStringType, inStringType, voidType));
		assertEquals(0x01000100, ParamRegisterType.packFromParamTypes(
				doubleValueType, inStringType, doubleValueType, voidType));
		assertEquals(0x02000100, ParamRegisterType.packFromParamTypes(
				floatValueType, inStringType, doubleValueType, voidType));
		assertEquals(0x01020200, ParamRegisterType.packFromParamTypes(
				doubleValueType, floatValueType, floatValueType, voidType));
		assertEquals(0x00010100, ParamRegisterType.packFromParamTypes(
				inStringType, doubleValueType, doubleValueType, voidType));
	}

	@Test
	public void testPack4Params() {
		assertEquals(0, ParamRegisterType.packFromParamTypes(inStringType,
				inStringType, voidType, voidType));
		assertEquals(1, ParamRegisterType.packFromParamTypes(inStringType,
				inStringType, inStringType, doubleValueType));
	}
}
