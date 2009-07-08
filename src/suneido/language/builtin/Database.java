package suneido.language.builtin;

import static suneido.database.server.Command.theDbms;

import java.math.BigDecimal;

import suneido.*;
import suneido.database.server.ServerData;
import suneido.language.*;

public class Database extends SuValue {

	private static final FunctionSpec requestFS = new FunctionSpec("request");
	@Override
	public Object call(Object... args) {
		args = Args.massage(requestFS, args);
		String request = Ops.toStr(args[0]);
		theDbms.admin(new ServerData(), request);
		return Boolean.TRUE;
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "<new>")
			throw new SuException("cannot create instances of Database");
		if (method == "CurrentSize")
			return currentSize(args);
		if (method == "Cursors")
			return cursors(args);
		if (method == "SessionId")
			return "127.0.0.1"; // TODO SessionId
		if (method == "TempDest")
			return 0; // not relevant to jSuneido
		if (method == "Transactions")
			return new SuContainer(); // TODO Transactions
		return super.invoke(self, method, args);
	}

	private Object currentSize(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		// need BigDecimal to handle long values
		return BigDecimal.valueOf(theDbms.size());
	}

	private int cursors(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return theDbms.cursors();
	}

}
