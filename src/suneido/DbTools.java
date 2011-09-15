/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

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

public class DbTools {

	public static void dumpDatabasePrint(DatabasePackage dbpkg, String dbFilename,
			String outputFilename) {
		Database db = dbpkg.openReadonly(dbFilename);
		try {
			int n = dumpDatabase(dbpkg, db, outputFilename);
			System.out.println("dumped " + n + " tables " +
					"from " + dbFilename + " to " + outputFilename);
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
		File tempfile = FileUtils.tempfile();
		Database db = dbpkg.create(tempfile.getPath());
		try {
			ReadableByteChannel fin = new FileInputStream(filename).getChannel();
			try {
				int n = dbpkg.loadDatabase(db, fin);
				db.close();
				FileUtils.renameWithBackup(tempfile, dbFilename);
				System.out.println("loaded " + n + " tables from " + filename +
						" into new " + dbFilename);
			} finally {
				fin.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("load failed", e);
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
		System.out.println("Checking " + dbFilename + " ...");
		return dbpkg.check(dbFilename, printObserver);
	}

	public static void compactPrintExit(DatabasePackage dbpkg, String dbFilename) {
		System.out.println("Checking " + dbFilename + " ...");
		Status status = dbpkg.check(dbFilename, printObserver);
		if (status != Status.OK)
			System.exit(-1);
		Database srcdb = dbpkg.openReadonly(dbFilename);
		try {
			File tempfile = FileUtils.tempfile();
			Database dstdb = dbpkg.create(tempfile.getPath());
			try {
				System.out.println("Compacting...");
				int n = dbpkg.compact(srcdb, dstdb);
				FileUtils.renameWithBackup(tempfile, dbFilename);
				System.out.println("Compacted " + n + " tables in " + dbFilename);
			} finally {
				dstdb.close();
			}
		} finally {
			srcdb.close();
		}
		System.exit(0);
	}

	public static void rebuildOrExit(DatabasePackage dbpkg, String dbFilename) {
		System.out.println("Rebuilding " + dbFilename + " ...");
		File tempfile = FileUtils.tempfile();
		String result = dbpkg.rebuild(dbFilename, tempfile.getPath());
		if (result == null)
			Suneido.fatal("Rebuild failed " + dbFilename + " UNRECOVERABLE");
		else if (Status.OK != dbpkg.check(tempfile.getPath(), printObserver))
			Suneido.fatal("Check failed after rebuild " + dbFilename + " " + result);
		else {
			Suneido.errlog("Rebuilt " + dbFilename + " " + result);
			System.out.println("Rebuild SUCCEEDED");
			FileUtils.renameWithBackup(tempfile, dbFilename);
		}
	}

}
