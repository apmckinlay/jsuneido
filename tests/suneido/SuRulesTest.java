/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.language.SuMethod0;

public class SuRulesTest {

	@Test
	public void test_put_get() {
		SuRules r = new SuRules();
		r.setDefault(null);
		assertEquals(null, r.get("nonexistent"));
		r.put("a", 123);
		assertEquals(123, r.get("a"));
		r.put("b", 456);
		assertEquals(123, r.get("a"));
		assertEquals(456, r.get("b"));
	}

	@Test
	public void test_no_dependencies() {
		SuRules r = new SuRules();
		r.attachRule("a", new SuMethod0() {
				@Override
				public Object eval0(Object self) {
					return "result";
				}
			});
		assertEquals("result", r.get("a"));
	}

	int count = 0;

	SuMethod0 with_deps = new SuMethod0() {
		@Override
		public Object eval0(Object self) {
			++count;
			SuRules r = (SuRules) self;
			r.get("a"); // get twice
			return r.get("a").toString() + r.get("b").toString();
		}
	};

	@Test
	public void test_dependencies() {
		SuRules r = new SuRules();
		r.attachRule("r", with_deps);
		r.put("a", 123);
		r.put("b", 456);
		assertEquals("123456", r.get("r"));
		assertEquals(1, count);
		assertEquals("123456", r.get("r"));
		assertEquals(1, count);
		r.put("b", "999");
		assertEquals(1, count);
		assertEquals("123999", r.get("r"));
		assertEquals(2, count);
		assertEquals("123999", r.get("r"));
		assertEquals(2, count);
		// putting same value should be ignored
		r.put("b", "999");
		assertEquals("123999", r.get("r"));
		assertEquals(2, count);

		r.invalidate("r");
		assertEquals("123999", r.get("r"));
		assertEquals(3, count);
	}

	int count1;
	int count2;

	@Test
	public void test_chained_rules() {
		SuRules r = new SuRules();
		// r1 => r2 => a
		r.attachRule("r1", new SuMethod0() {
			@Override
			public Object eval0(Object self) {
				++count1;
				return "=" + ((SuRules) self).get("r2").toString();
			}});
		r.attachRule("r2", new SuMethod0() {
			@Override
			public Object eval0(Object self) {
				++count2;
				return ((SuRules) self).get("a").toString().substring(0, 1); // first char
			}});
		r.put("a", "xy");
		assertEquals("=x", r.get("r1"));
		assertEquals(1, count1);
		assertEquals(1, count2);
		r.put("a", "XY");
		assertEquals("=X", r.get("r1"));
		assertEquals(2, count1);
		assertEquals(2, count2);
		r.get("r2"); // make it valid
		r.put("a", "XZ");
		assertEquals("=X", r.get("r1"));
		assertEquals(3, count2);
		assertEquals(2, count1);

		assertEquals("r2", r.getdeps("r1"));
		assertEquals("a", r.getdeps("r2"));
		assertEquals("", r.getdeps("a"));
	}

	@Test
	public void test_set_deps() {
		SuRules r = new SuRules();
		r.attachRule("r", with_deps);
		r.put("r", 123);
		assertEquals(123, r.get("r"));
		assertEquals(0, count);
		r.setdeps("r", "a,b");
		assertEquals("a,b", r.getdeps("r").toString());
		r.put("a", 12);
		r.put("b", 34);
		assertEquals("1234", r.get("r"));
	}

	@Test
	public void test_invalidate_bug() {
		SuRules r = new SuRules();
		r.put("a", 123);
		assertEquals(123, r.get("a"));
		r.invalidate("a");
		assertEquals(123, r.get("a"));
	}

}
