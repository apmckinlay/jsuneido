/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.DatabasePackage.Status;

interface ImmuDatabase extends suneido.intfc.database.Database {

	@Override
	ImmuReadTran readonlyTran();

	@Override
	ImmuUpdateTran readwriteTran();

	ImmuExclTran exclusiveTran();

	Status check();

}
