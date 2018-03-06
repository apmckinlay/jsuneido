/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.database.immudb.Dbpkg.printObserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.google.common.base.Stopwatch;

import suneido.database.immudb.Database;
import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.Dbpkg.Status;
import suneido.util.Errlog;
import suneido.util.FileUtils;
import suneido.util.Jvm;

public class DbTools {
	private static final String SEPARATOR = "!!";

	public static void dumpPrintExit(String dbFilename, String outputFilename) {
		if (Status.OK != checkPrint(dbFilename)) {
			System.out.println("Dump ABORTED - check failed - database CORRUPT");
			System.exit(-1);
		}
		try (Database db = Dbpkg.openReadonly(dbFilename)) {
			Stopwatch sw = Stopwatch.createStarted();
			int n = dumpDatabase(db, outputFilename);
			System.out.println("dumped " + n + " tables " +
					"from " + dbFilename + " to " + outputFilename +
					" in " + sw);
		}
	}

	public static int dumpDatabase(Database db, String outputFilename) {
		int ntables;
		String tempfile = FileUtils.tempfile().toString();
		try (FileOutputStream fout = new FileOutputStream(tempfile)) {
			ntables = Dbpkg.dumpDatabase(db, fout.getChannel());
		} catch (Exception e) {
			throw new RuntimeException("dump failed", e);
		}
		FileUtils.renameWithBackup(tempfile, outputFilename);
		return ntables;
	}

	public static void dumpTablePrint(String dbFilename, String tablename) {
		try (Database db = Dbpkg.openReadonly(dbFilename)) {
			int n = dumpTable(db, tablename);
			System.out.println("dumped " + n + " records " +
					"from " + tablename + " to " + tablename + ".su");
		}
	}

	public static int dumpTable(Database db, String tablename) {
		try (FileOutputStream fout = new FileOutputStream(tablename + ".su")) {
			return Dbpkg.dumpTable(db, tablename, fout.getChannel());
		} catch (Exception e) {
			throw new RuntimeException("dump table failed", e);
		}
	}

	public static void loadDatabasePrint(String dbFilename, String filename) {
		String tempfile = FileUtils.tempfile("d", "i", "c").toString();
		if (! Jvm.runWithNewJvm("-load:" + filename + SEPARATOR + tempfile))
			Errlog.fatal("Load FAILED");
		if (! Jvm.runWithNewJvm("-check:" + tempfile))
			Errlog.fatal("Load ABORTED - check failed after load");
		Dbpkg.renameDbWithBackup(tempfile, dbFilename);
	}

	static void load2(String arg) {
		int i = arg.indexOf(SEPARATOR);
		String filename = arg.substring(0, i);
		String tempfile = arg.substring(i + SEPARATOR.length());
		// FIXME: The three lines above just assume the existence of SEPARATOR.
		//        But if it's not there, i == -1 and this function throws an
		//        obscure -- in the sense of non-informational --
		//        StringIndexOutOfBoundsError...
		try (Database db = Dbpkg.create(tempfile);
				FileInputStream fin = new FileInputStream(filename)) {
			Stopwatch sw = Stopwatch.createStarted();
			int n = Dbpkg.loadDatabase(db, fin.getChannel());
			System.out.println("loaded " + n + " tables from " + filename +
					" in " + sw);
		} catch (Exception e) {
			throw new RuntimeException("load failed", e);
		}
	}

	public static void loadTablePrint(String dbFilename, String tablename) {
		try (Database db = Dbpkg.dbExists(dbFilename)
				? Dbpkg.open(dbFilename) : Dbpkg.create(dbFilename)) {
			if (db == null)
				throw new RuntimeException("can't open database");
			int n = loadTable(db, tablename);
			tablename = stripsu(tablename);
			System.out.println("loaded " + n + " records " +
					"from " + tablename + ".su into " + tablename + " in " + dbFilename);
		} catch (Exception e) {
			throw new RuntimeException("load " + tablename + " failed", e);
		}
	}

	public static int loadTable(Database db, String tablename) {
		tablename = stripsu(tablename);
		try (FileInputStream fin = new FileInputStream(tablename + ".su")) {
			return Dbpkg.loadTable(db, tablename, fin.getChannel());
		} catch (Exception e) {
			throw new RuntimeException("load " + tablename + " failed", e);
		}
	}

	private static String stripsu(String tablename) {
		if (tablename.endsWith(".su"))
			tablename = tablename.substring(0, tablename.length() - 3);
		return tablename;
	}

	public static void checkPrintExit(String dbFilename) {
		Status status = checkPrint(dbFilename);
		System.exit(status == Status.OK ? 0 : -1);
	}

	public static Status checkPrint(String dbFilename) {
		System.out.println("Checking " +
				(dbFilename.endsWith(".tmp") ? "" : dbFilename + " ") + "...");
		Stopwatch sw = Stopwatch.createStarted();
		Status result = Dbpkg.check(dbFilename, printObserver);
		System.out.println("Checked in " + sw);
		return result;
	}

	public static void compactPrintExit(String dbFilename) {
		if (! Jvm.runWithNewJvm("-check:" + dbFilename))
			Errlog.fatal("Compact ABORTED - check failed before compact - database CORRUPT");
		String tempfile = FileUtils.tempfile("d", "i", "c").toString();
		if (! Jvm.runWithNewJvm("-compact:" + dbFilename + SEPARATOR + tempfile))
			Errlog.fatal("Compact FAILED");
		if (! Jvm.runWithNewJvm("-check:" + tempfile))
			Errlog.fatal("Compact ABORTED - check failed after compact");
		Dbpkg.renameDbWithBackup(tempfile, dbFilename);
	}

	static void compact2(String arg) {
		int i = arg.indexOf(SEPARATOR);
		String dbFilename = arg.substring(0, i);
		String tempfile = arg.substring(i + SEPARATOR.length());
		try (Database srcdb = Dbpkg.openReadonly(dbFilename);
				Database dstdb = Dbpkg.create(tempfile)) {
			System.out.printf("size before: %,d%n", srcdb.size());
			System.out.println("Compacting...");
			Stopwatch sw = Stopwatch.createStarted();
			int n = Dbpkg.compact(srcdb, dstdb);
			System.out.println("Compacted " + n + " tables in " + dbFilename +
					" in " + sw);
			System.out.printf("size after: %,d%n", dstdb.size());
		}
	}

	public static void rebuildOrExit(String dbFilename) {
		System.out.println("Rebuild " + dbFilename + " ...");
		File tempfile = FileUtils.tempfile("d", "i", "c");
		String cmd = "-rebuild:" + dbFilename + SEPARATOR + tempfile;
		boolean dbi = new File(dbFilename + "i").canRead();
		if (dbi && Jvm.runWithNewJvm(cmd)) {
			if (! tempfile.isFile())
				return; // assume db was ok, rebuild not needed
		} else {
			if (! dbi)
				System.out.println("No usable indexes");
			// couldn't rebuild from data + indexes, try from just data
			cmd = "-rebuild-" + dbFilename + SEPARATOR + tempfile;
			if (! Jvm.runWithNewJvm(cmd))
				Errlog.fatal("Rebuild FAILED " + cmd);
		}
		Dbpkg.renameDbWithBackup(tempfile.toString(), dbFilename);
		tempfile.delete();
	}

	/** called in a new jvm */
	static void rebuild2(String arg) {
		int i = arg.indexOf(SEPARATOR);
		String dbFilename = arg.substring(0, i);
		String tempfile = arg.substring(i + SEPARATOR.length());
		Stopwatch sw = Stopwatch.createStarted();
		String result = Dbpkg.rebuild(dbFilename, tempfile);
		if (result == null)
			System.exit(-1);
		else if (new File(tempfile).isFile()){
			Errlog.warn("Rebuilt " + dbFilename + ": " + result);
			System.out.println("Rebuild completed in " + sw);
		} else
			System.out.println("Rebuild not done, database OK");
	}

	/** called in a new jvm */
	static void rebuild3(String arg) {
		int i = arg.indexOf(SEPARATOR);
		String dbFilename = arg.substring(0, i);
		String tempfile = arg.substring(i + SEPARATOR.length());
		Stopwatch sw = Stopwatch.createStarted();
		String result = Dbpkg.rebuildFromData(dbFilename, tempfile);
		if (result == null)
			System.exit(-1);
		else {
			Errlog.warn("Rebuilt " + dbFilename + ": " + result);
			System.out.println("Rebuild from data completed in " + sw);
		}
	}

}
