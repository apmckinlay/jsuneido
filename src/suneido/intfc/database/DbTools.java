/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

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
				FileUtils.renameWithBackup(tempfile, dbFilename);
				System.out.println("loaded " + n + " tables from " + filename +
						" into new " + dbFilename);
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

}
