/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Suneido.dbpkg;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import suneido.TheDbms;
import suneido.intfc.database.Database;
import suneido.intfc.database.Record;
import suneido.intfc.database.Table;
import suneido.intfc.database.Transaction;
import suneido.util.Util;

public class Auth {
	private static SecureRandom random = new SecureRandom();
	public static final int NONCE_SIZE = 8;
	public static final int TOKEN_SIZE = 16;
	private static Set<String> tokens = Sets.newConcurrentHashSet();

//	boolean haveUserTable() {
//		Database db = ((DbmsLocal) TheDbms.dbms()).getDb();
//		Transaction t = db.readTransaction();
//		try {
//			return ! t.tableExists("users");
//		} finally {
//			t.complete();
//		}
//	}

	/** @return Random bytes */
	synchronized private static byte[] random(int size) {
		byte bytes[] = new byte[size];
		random.nextBytes(bytes);
		return bytes;
	}

	/** @return 8 random bytes used to salt the auth hash */
	public static byte[] nonce() {
		byte[] nonce = random(NONCE_SIZE);
		ServerData.forThread().setNonce(nonce);
		return nonce;
	}

	/** @return a one time random token that can be used to authorize */
	// must not throw because client-server protocol won't handle it
	public static byte[] token() {
		if (! ServerData.forThread().auth)
			return new byte[TOKEN_SIZE];
		byte[] token = random(TOKEN_SIZE);
		tokens.add(Util.bytesToString(token));
		return token;
	}

	/**
	 * data may be token or user + '\x00' + sha1(nonce + lookup(user).passhash)
	 * where passhash is md5(user + password)
	 */
	public static boolean auth(String data) {
		if (! (isToken(data) || isUser(data)))
			return false;
		ServerData.forThread().auth = true;
		return true;
	}

	private static boolean isToken(String data) {
		return tokens.remove(data);
	}

	/** separator between user and password hash */
	static final char SEPARATOR = 0;

	private static boolean isUser(String data) {
		byte[] nonce = ServerData.forThread().getNonce();
		if (nonce == null)
			return false;
		int i = data.indexOf(SEPARATOR);
		if (i == -1)
			return false;
		String user = data.substring(0, i);
		String hash = data.substring(i + 1);

		String passHash = getPassHash(user);
		String shouldBe = sha1(Util.bytesToString(nonce) + passHash);

		return hash.equals(shouldBe);
	}

	/**
	 * Requires a 'users' table with fields 'user' and 'passhash'
	 * with a key on 'user'
	 * @return The value of the passhash field for the specified user
	 * or "" if it fails.
	 */
	private static String getPassHash(String user) {
		Record key = dbpkg.recordBuilder().add(user).build();
		Database db = ((DbmsLocal) TheDbms.dbms()).getDb();
		Transaction t = db.readTransaction();
		try {
			Table table = t.getTable("users");
			if (table == null)
				return "";
			List<String> flds = table.getFields();
			int pass_fld = flds.indexOf("passhash");
			if (pass_fld < 0)
				return "";
			Record rec = t.lookup(table.num(), "user", key);
			if (rec == null)
				return "";
			return rec.getString(pass_fld);
		} finally {
			t.complete();
		}
	}

	private static String sha1(String s) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("can't get SHA1");
		}
		md.update(Util.stringToBytes(s));
		return Util.bytesToString(md.digest());
	}

}
