/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class CommandLineOptionsTest {

	@Test
	public void none() {
		assertEquals("REPL",
				CommandLineOptions.parse().toString());
	}

	@Test
	public void just_rest() {
		assertEquals("REPL rest: foo bar",
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
		assertEquals("REPL rest: -foo -bar",
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
				CommandLineOptions.parse("-s", "-port", "1234").toString());
		assertEquals("SERVER port=1234",
				CommandLineOptions.parse("-s", "-p", "1234").toString());
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
		assertEquals("ERROR port should only be specifed with -server or -client, not DUMP",
				CommandLineOptions.parse("-dump", "-port",  "123").toString());
	}

	@Test
	public void unknown() {
		assertEquals("ERROR unknown option: -abc",
				CommandLineOptions.parse("-abc").toString());
	}

	@Test
	public void timeout() {
		assertThat(CommandLineOptions.parse("-timeout", "1234").toString(),
				is("REPL timeout=1234"));
		assertThat(CommandLineOptions.parse("-to", "1234").toString(),
				is("REPL timeout=1234"));
	}

}
