package suneido.language.jsdi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import static suneido.util.testing.Throwing.*;

/**
 * Test for {@link MarshallPlan}.
 *
 * @author Victor Schappert
 * @since 20130716
 */
@DllInterface
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
	public void testNullPlans() {
		assertThrew(new Runnable() {
			public void run() {
				MarshallPlan.makeDirectPlan(0);
			}
		}, IllegalArgumentException.class);
		final MarshallPlan NULL_PLAN = MarshallPlan.makeContainerPlan(Arrays
				.asList(new MarshallPlan[0]));
		assertEquals(0, NULL_PLAN.getSizeDirectStack());
		assertThrew(new Runnable() {
			public void run() {
				MarshallPlan.makePointerPlan(NULL_PLAN);
			}
		}, IllegalArgumentException.class);
		assertThrew(new Runnable() {
			public void run() {
				MarshallPlan.makeArrayPlan(NULL_PLAN, 3);
			}
		}, IllegalArgumentException.class);
	}

	@Test
	public void testDirect() {
		assertEquals(
			"MarshallPlan[ 1, 0, { }, { 0 }, #vi:0 ]",
			DIRECT1.toString()
		);
	}

	@Test
	public void testDirectPointer() {
		assertEquals(
			"MarshallPlan[ 4, 1, { 0:4 }, { 0, 4 }, #vi:0 ]",
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
			"MarshallPlan[ 2, 0, { }, { 0, 1 }, #vi:0 ]",
			ARRAY1_2.toString()
		);
	}

	@Test
	public void testDirectContainer() {
		assertEquals(cp(DIRECT1), DIRECT1);
		assertEquals(
			"MarshallPlan[ 7, 0, { }, { 0, 1, 3 }, #vi:0 ]",
			CONT1_2_4.toString()
		);
	}

	@Test
	public void testDirectWithVariableIndirect() {
		assertEquals(
			"MarshallPlan[ 4, 0, { 0:4 }, { 0 }, #vi:1 ]",
			MarshallPlan.makeVariableIndirectPlan().toString()
		);
	}

	@Test
	public void testPointerPointer() {
		MarshallPlan ptrPtrPlan = MarshallPlan.makePointerPlan(PTR1);
		assertEquals(
			"MarshallPlan[ 4, 5, { 0:4, 4:8 }, { 0, 4, 8 }, #vi:0 ]",
			ptrPtrPlan.toString()
		);
	}

	@Test
	public void testPointerArray() {
		MarshallPlan ptrArrayPlan = MarshallPlan.makeArrayPlan(PTR1, 3);
		assertEquals(
			"MarshallPlan[ 12, 3, { 0:12, 4:13, 8:14 }, { 0, 12, 4, 13, 8, 14 }, #vi:0 ]",
			ptrArrayPlan.toString()
		);
	}

	@Test
	public void testPointerContainer() {
		assertEquals(cp(PTR1), PTR1);
		MarshallPlan ptrContainerPlan = cp(PTR1, PTR1, PTR1);
		assertEquals(
			"MarshallPlan[ 12, 3, { 0:12, 4:13, 8:14 }, { 0, 12, 4, 13, 8, 14 }, #vi:0 ]",
			ptrContainerPlan.toString()
		);
	}

	@Test
	public void testArrayPointer() {
		MarshallPlan arrayPtrPlan = MarshallPlan.makePointerPlan(ARRAY1_2);
		assertEquals(
			"MarshallPlan[ 4, 2, { 0:4 }, { 0, 4, 5 }, #vi:0 ]",
			arrayPtrPlan.toString()
		);
	}

	@Test
	public void testArrayArray() {
		assertEquals(MarshallPlan.makeArrayPlan(ARRAY1_2, 1), ARRAY1_2);
		MarshallPlan arrayArrayPlan = MarshallPlan.makeArrayPlan(ARRAY1_2, 2);
		assertEquals(
			"MarshallPlan[ 4, 0, { }, { 0, 1, 2, 3 }, #vi:0 ]",
			arrayArrayPlan.toString()
		);
	}

	@Test
	public void testArrayContainer() {
		assertEquals(cp(ARRAY1_2), ARRAY1_2);
		MarshallPlan arrayContainerPlan = cp(ARRAY1_2, ARRAY1_2);
		assertEquals(
			"MarshallPlan[ 4, 0, { }, { 0, 1, 2, 3 }, #vi:0 ]",
			arrayContainerPlan.toString()
		);
	}

	@Test
	public void testContainerPointer() {
		MarshallPlan containerPtrPlan = MarshallPlan.makePointerPlan(CONT1_2_4);
		assertEquals(
			"MarshallPlan[ 4, 7, { 0:4 }, { 0, 4, 5, 7 }, #vi:0 ]",
			containerPtrPlan.toString()
		);
	}

	@Test
	public void testContainerArray() {
		MarshallPlan containerArrayPlan = MarshallPlan.makeArrayPlan(CONT1_2_4, 4);
		assertEquals(
			"MarshallPlan[ 28, 0, { }, { 0, 1, 3, 7, 8, 10, 14, 15, 17, 21, 22, 24 }, #vi:0 ]",
			containerArrayPlan.toString()
		);
	}

	@Test
	public void testContainerContainer() {
		assertEquals(cp(CONT1_2_4), CONT1_2_4);
		MarshallPlan contContPlan = cp(CONT1_2_4, CONT1_2_4);
		assertEquals(
			"MarshallPlan[ 14, 0, { }, { 0, 1, 3, 7, 8, 10 }, #vi:0 ]",
			contContPlan.toString()
		);
	}
}
