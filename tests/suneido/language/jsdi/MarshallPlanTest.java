package suneido.language.jsdi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test for {@link MarshallPlan}.
 *
 * @author Victor Schappert
 * @since 20130716
 */
public class MarshallPlanTest {

	private static MarshallPlan cp(MarshallPlan ... childPlans) {
		return MarshallPlan.makeContainerPlan(Arrays.asList(childPlans));
	}

	private static final MarshallPlan DIRECT1 = MarshallPlan.makeDirectPlan(1);
	private static final MarshallPlan PTR1 = MarshallPlan.makePointerPlan(DIRECT1);
	private static final MarshallPlan ARRAY1_2 = MarshallPlan.makeArrayPlan(DIRECT1, 2);
	private static final MarshallPlan CONT1_2_4 = cp(DIRECT1,
			MarshallPlan.makeDirectPlan(2), MarshallPlan.makeDirectPlan(4));

	@Test
	public void testDirect() {
		assertEquals(
			"MarshallPlan[ 1, 0, { }, { 0 }, no-vi ]",
			DIRECT1.toString()
		);
	}

	@Test
	public void testDirectPointer() {
		assertEquals(
			"MarshallPlan[ 4, 1, { 0:4 }, { 0, 4 }, no-vi ]",
			PTR1.toString()
		);
	}

	@Test
	public void testDirectArray() {
		assertEquals(
			MarshallPlan.makeArrayPlan(DIRECT1, 1),
			DIRECT1
		);
		assertEquals(
			"MarshallPlan[ 2, 0, { }, { 0, 1 }, no-vi ]",
			ARRAY1_2.toString()
		);
	}

	@Test
	public void testDirectContainer() {
		assertEquals(cp(DIRECT1), DIRECT1);
		assertEquals(
			"MarshallPlan[ 7, 0, { }, { 0, 1, 3 }, no-vi ]",
			CONT1_2_4.toString()
		);
	}

	@Test
	public void testDirectWithVariableIndirect() {
		assertEquals(
			"MarshallPlan[ 4, 0, { 0:-1 }, { 0, -1 }, vi ]",
			MarshallPlan.makeVariableIndirectPlan().toString()
		);
	}

	@Test
	public void testPointerPointer() {
		MarshallPlan ptrPtrPlan = MarshallPlan.makePointerPlan(PTR1);
		assertEquals(
			"MarshallPlan[ 4, 5, { 0:4, 4:8 }, { 0, 4, 8 }, no-vi ]",
			ptrPtrPlan.toString()
		);
	}

	@Test
	public void testPointerArray() {
		MarshallPlan ptrArrayPlan = MarshallPlan.makeArrayPlan(PTR1, 3);
		assertEquals(
			"MarshallPlan[ 12, 3, { 0:12, 4:13, 8:14 }, { 0, 12, 4, 13, 8, 14 }, no-vi ]",
			ptrArrayPlan.toString()
		);
	}

	@Test
	public void testPointerContainer() {
		assertEquals(cp(PTR1), PTR1);
		MarshallPlan ptrContainerPlan = cp(PTR1, PTR1, PTR1);
		assertEquals(
			"MarshallPlan[ 12, 3, { 0:12, 4:13, 8:14 }, { 0, 12, 4, 13, 8, 14 }, no-vi ]",
			ptrContainerPlan.toString()
		);
	}

	@Test
	public void testArrayPointer() {
		MarshallPlan arrayPtrPlan = MarshallPlan.makePointerPlan(ARRAY1_2);
		assertEquals(
			"MarshallPlan[ 4, 2, { 0:4 }, { 0, 4, 5 }, no-vi ]",
			arrayPtrPlan.toString()
		);
	}

	@Test
	public void testArrayArray() {
		assertEquals(MarshallPlan.makeArrayPlan(ARRAY1_2, 1), ARRAY1_2);
		MarshallPlan arrayArrayPlan = MarshallPlan.makeArrayPlan(ARRAY1_2, 2);
		assertEquals(
			"MarshallPlan[ 4, 0, { }, { 0, 1, 2, 3 }, no-vi ]",
			arrayArrayPlan.toString()
		);
	}

	@Test
	public void testArrayContainer() {
		assertEquals(cp(ARRAY1_2), ARRAY1_2);
		MarshallPlan arrayContainerPlan = cp(ARRAY1_2, ARRAY1_2);
		assertEquals(
			"MarshallPlan[ 4, 0, { }, { 0, 1, 2, 3 }, no-vi ]",
			arrayContainerPlan.toString()
		);
	}

	@Test
	public void testContainerPointer() {
		MarshallPlan containerPtrPlan = MarshallPlan.makePointerPlan(CONT1_2_4);
		assertEquals(
			"MarshallPlan[ 4, 7, { 0:4 }, { 0, 4, 5, 7 }, no-vi ]",
			containerPtrPlan.toString()
		);
	}

	@Test
	public void testContainerArray() {
		MarshallPlan containerArrayPlan = MarshallPlan.makeArrayPlan(CONT1_2_4, 4);
		assertEquals(
			"MarshallPlan[ 28, 0, { }, { 0, 1, 3, 7, 8, 10, 14, 15, 17, 21, 22, 24 }, no-vi ]",
			containerArrayPlan.toString()
		);
	}

	@Test
	public void testContainerContainer() {
		assertEquals(cp(CONT1_2_4), CONT1_2_4);
		MarshallPlan contContPlan = cp(CONT1_2_4, CONT1_2_4);
		assertEquals(
			"MarshallPlan[ 14, 0, { }, { 0, 1, 3, 7, 8, 10 }, no-vi ]",
			contContPlan.toString()
		);
	}
}
