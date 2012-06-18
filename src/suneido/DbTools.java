/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.Suneido.errlog;
import static suneido.Suneido.fatal;
import static suneido.intfc.database.DatabasePackage.printObserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import suneido.intfc.database.Database;
import suneido.intfc.database.DatabasePackage;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.util.FileUtils;
import suneido.util.Jvm;

import com.google.common.base.Stopwatch;

public class DbTools {
	private static final String SEPARATOR = "!!";

	public static void dumpDatabasePrint(DatabasePackage dbpkg, String dbFilename,
			String outputFilename) {
		Database db = dbpkg.openReadonly(dbFilename);
		try {
			Stopwatch sw = new Stopwatch().start();
			int n = dumpDatabase(dbpkg, db, outputFilename);
			System.out.println("dumped " + n + " tables " +
					"from " + dbFilename + " to " + outputFilename +
					" in " + sw);
		} finally {
			db.close();
		}
	}

	public static int dumpDatabase(DatabasePackage dbpkg, Database db,
			String outputFilename) {
		try {
			WritableByteChannel fout = new FileOutputStream(outputFilename).getChannel();
			try {
				return dbpkg.dumpDatabase(db, fout);
			} finally {
				fout.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("dump failed", e);
		}
	}

	public static void dumpTablePrint(DatabasePackage dbpkg, String dbFilename,
			String tablename) {
		Database db = dbpkg.openReadonly(dbFilename);
		try {
			int n = dumpTable(dbpkg, db, tablename);
			System.out.println("dumped " + n + " records " +
					"from " + tablename + " to " + tablename + ".su");
		} finally {
			db.close();
		}
	}

	public static int dumpTable(DatabasePackage dbpkg, Database db,
			String tablename) {
		try {
			WritableByteChannel fout = new FileOutputStream(tablename + ".su").getChannel();
			try {
				return dbpkg.dumpTable(db, tablename, fout);
			} finally {
				fout.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("dump table failed", e);
		}
	}

	public static void loadDatabasePrint(DatabasePackage dbpkg, String dbFilename,
			String filename) {
		String tempfile = FileUtils.tempfile().toString();
		if (! Jvm.runWithNewJvm("-load:" + filename + SEPARATOR + tempfile))
			System.exit(-1);
		if (! Jvm.runWithNewJvm("-check:" + tempfile))
			fatal("Check failed after Load " + dbFilename);
		dbpkg.renameDbWithBackup(tempfile, dbFilename);
	}

	static void load2(DatabasePackage dbpkg, String arg) {
		int i = arg.indexOf(SEPARATOR);
		String filename = arg.substring(0, i);
		String tempfile = arg.substring(i + SEPARATOR.length());
		Database db = dbpkg.create(tempfile);
		try {
			ReadableByteChannel fin = new FileInputStream(filename).getChannel();
			try {
				Stopwatch sw = new Stopwatch().start();
				int n = dbpkg.loadDatabase(db, fin);
				System.out.println("loaded " + n + " tables from " + filename +
						" in " + sw);
			} finally {
				fin.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("load failed", e);
		} finally {
			db.close();
		}
	}

	public static void loadTablePrint(DatabasePackage dbpkg, String dbFilename,
			String tablename) {
		if (tablename.endsWith(".su"))
			tablename = tablename.substring(0, tablename.length() - 3);
		Database db = new File(dbFilename).exists()
				? dbpkg.open(dbFilename) : dbpkg.create(dbFilename);
		try {
			ReadableByteChannel fin = new FileInputStream(tablename + ".su").getChannel();
			try {
				int n = dbpkg.loadTable(db, tablename, fin);
				System.out.println("loaded " + n + " records " +
						"from " + tablename + ".su into " + tablename + " in " + dbFilename);
			} finally {
				fin.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("load table failed", e);
		} finally {
			db.close();
		}
	}

	public static void checkPrintExit(DatabasePackage dbpkg, String dbFilename) {
		Status status = checkPrint(dbpkg, dbFilename);
		System.exit(status == Status.OK ? 0 : -1);
	}

	public static Status checkPrint(DatabasePackage dbpkg, String dbFilename) {
		System.out.println("Checking " +
				(dbFilename.endsWith(".tmp") ? "" : dbFilename + " ") + "...");
		return dbpkg.check(dbFilename, printObserver);
	}

	public static void compactPrintExit(DatabasePackage dbpkg, String dbFilename) {
		if (! Jvm.runWithNewJvm("-check:" + dbFilename))
			System.exit(-1);
		String tempfile = FileUtils.tempfile().toString();
		if (! Jvm.runWithNewJvm("-compact:" + dbFilename + SEPARATOR + tempfile))
			System.exit(-1);
		if (! Jvm.runWithNewJvm("-check:" + tempfile))
			fatal("Check failed after Compact " + dbFilename);
		dbpkg.renameDbWithBackup(tempfile, dbFilename);
	}

	static void compact2(DatabasePackage dbpkg, String arg) {
		int i = arg.indexOf(SEPARATOR);
		String dbFilename = arg.substring(0, i);
		String tempfile = arg.substring(i + SEPARATOR.length());
		Database srcdb = dbpkg.openReadonly(dbFilename);
		try {
			Database dstdb = dbpkg.create(tempfile);
			try {
				System.out.println("Compacting...");
				Stopwatch sw = new Stopwatch().start();
				int n = dbpkg.compact(srcdb, dstdb);
				System.out.println("Compacted " + n + " tables in " + dbFilename +
						" in " + sw);
			} finally {
				dstdb.close();
			}
		} finally {
			srcdb.close();
		}
	}

	public static void rebuildOrExit(DatabasePackage dbpkg, String dbFilename) {
		System.out.println("Rebuilding " + dbFilename + " ...");
		String tempfile = FileUtils.tempfile().toString();
		if (! Jvm.runWithNewJvm("-rebuild:" + dbFilename + SEPARATOR + tempfile))
			fatal("Rebuild failed " + dbFilename);
		if (! Jvm.runWithNewJvm("-check:" + tempfile))
			fatal("Check failed after Rebuild " + dbFilename);
		dbpkg.renameDbWithBackup(tempfile, dbFilename);
	}

	/** called in the new jvm */
	static void rebuild2(DatabasePackage dbpkg, String arg) {
		int i = arg.indexOf(SEPARATOR);
		String dbFilename = arg.substring(0, i);
		String tempfile = arg.substring(i + SEPARATOR.length());
		Stopwatch sw = new Stopwatch().start();
		String result = dbpkg.rebuild(dbFilename, tempfile);
		if (result == null)
			fatal("Rebuild " + dbFilename + ": FAILED");
		else {
			errlog("Rebuild " + dbFilename + ": " + result);
			System.out.println("Rebuild SUCCEEDED in " + sw);
		}
	}

}
