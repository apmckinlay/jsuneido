/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.util.List;

import suneido.SuContainer;
import suneido.SuDate;
import suneido.SuException;

/**
 * Wrapper for Dbms for unauthorized client connections.
 * <p>
 * Only allows sessionid, use, libget, nonce, and auth.
 */
public class DbmsUnauth extends Dbms {
	private final Dbms dbms;
	public final SuException notauth = new SuException("not authorized");

	public DbmsUnauth(Dbms dbms) {
		this.dbms = dbms;
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		throw notauth;
	}

	@Override
	public void admin(String s) {
		throw notauth;
	}

	@Override
	public DbmsQuery cursor(String s) {
		throw notauth;
	}

	@Override
	public List<Integer> transactions() {
		throw notauth;
	}

	@Override
	public SuDate timestamp() {
		throw notauth;
	}

	@Override
	public String check() {
		throw notauth;
	}

	@Override
	public String dump(String filename) {
		throw notauth;
	}

	@Override
	public int load(String filename) {
		throw notauth;
	}

	@Override
	public Object run(String s) {
		throw notauth;
	}

	@Override
	public long size() {
		throw notauth;
	}

	@Override
	public SuContainer connections() {
		throw notauth;
	}

	@Override
	public int cursors() {
		throw notauth;
	}

	@Override
	public String sessionid(String s) {
		return dbms.sessionid(s);
	}

	@Override
	public int finalSize() {
		throw notauth;
	}

	@Override
	public void log(String s) {
		throw notauth;
	}

	@Override
	public int kill(String s) {
		throw notauth;
	}

	@Override
	public Object exec(SuContainer c) {
		throw notauth;
	}

	@Override
	public List<LibGet> libget(String name) {
		return dbms.libget(name);
	}

	@Override
	public boolean use(String library) {
		return dbms.use(library);
	}

	@Override
	public boolean unuse(String library) {
		throw notauth;
	}

	@Override
	public List<String> libraries() {
		throw notauth;
	}

	@Override
	public void disableTrigger(String table) {
		throw notauth;
	}

	@Override
	public void enableTrigger(String table) {
		throw notauth;
	}

	@Override
	public byte[] nonce() {
		return dbms.nonce();
	}

	@Override
	public boolean auth(String data) {
		return dbms.auth(data);
	}

	@Override
	public byte[] token() {
		throw notauth;
	}

}
