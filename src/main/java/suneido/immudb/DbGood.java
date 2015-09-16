/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.google.common.primitives.Longs;

import suneido.util.Errlog;

/**
 * jSuneido checks the database at startup.
 * It normally does a fast check of the last part of the database.
 * But if the database is not shut down properly,
 * or signs of corruption are detected,
 * then next time it starts we want to do a full check.
 * To record that the database was shut down properly
 * a file (suneido.dbc) is written containing a hash of the database size.
 * At startup we check this file.
 * So if we crash after writing to the db then we'll do a full check.
 * If the db isn't modified then the size will stay the same
 * and we don't need a full check even if we crash.
 */
public class DbGood {

	public static void create(String filename, long size) {
		try {
			Files.write(Paths.get(filename), hash(size));
		} catch (Throwable e) {
			Errlog.errlog("ERROR in DbGood.create", e);
		}
	}

	public static boolean check(String filename, long size) {
		try {
			return new File(filename).canRead() &&
					Arrays.equals(
						hash(size),
						Files.readAllBytes(Paths.get(filename)));
		} catch (Throwable e) {
			Errlog.errlog("ERROR in DbGood.check", e);
			return false;
		}
	}

	private static byte[] hash(long size) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(Longs.toByteArray(size));
		return md.digest();
	}

}
