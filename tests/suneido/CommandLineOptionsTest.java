package suneido;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CommandLineOptionsTest {

	@Test
	public void none() {
		assertEquals("SERVER",
				CommandLineOptions.parse().toString());
	}

	@Test
	public void just_rest() {
		assertEquals("SERVER rest: foo bar",
				CommandLineOptions.parse("foo", "bar").toString());
	}

	@Test
	public void just_action() {
		assertEquals("DUMP",
				CommandLineOptions.parse("-dump").toString());
		assertEquals("SERVER",
				CommandLineOptions.parse("-s").toString());
	}

	@Test
	public void separator() {
		assertEquals("SERVER rest: -foo -bar",
				CommandLineOptions.parse("--", "-foo", "-bar").toString());
	}

	@Test
	public void action_arg() {
		assertEquals("DUMP stdlib",
				CommandLineOptions.parse("-dump", "stdlib").toString());
	}

	@Test
	public void port() {
		assertEquals("SERVER port=1234",
				CommandLineOptions.parse("-port", "1234").toString());
		assertEquals("SERVER port=1234",
				CommandLineOptions.parse("-p", "1234").toString());
		assertEquals("SERVER port=1234",
				CommandLineOptions.parse("-s", "-p", "1234").toString());
	}

	@Test
	public void two_actions() {
		assertEquals("ERROR only one action is allowed, cannot have both DUMP and LOAD",
				CommandLineOptions.parse("-dump", "-load").toString());
	}

	@Test
	public void port_without_server() {
		assertEquals("ERROR port should only be specifed with server, not DUMP",
				CommandLineOptions.parse("-dump", "-port",  "123").toString());
	}

	@Test
	public void unknown() {
		assertEquals("ERROR unknown option: -abc",
				CommandLineOptions.parse("-abc").toString());
	}

}
