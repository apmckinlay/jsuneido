package suneido;

import static org.junit.Assert.fail;

import org.junit.Test;

public class AssertTest {

	@Test
	public void test_assert_enabled() {
		try {
			assert false;
		} catch (AssertionError e) {
			return;
		}
		fail("assert not enabled");
	}

}
