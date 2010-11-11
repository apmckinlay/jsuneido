package suneido.language.builtin;

import static suneido.Suneido.theDbms;
import static suneido.util.Util.array;

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
		theDbms.admin(ServerData.forThread(), request);
		return Boolean.TRUE;
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "<new>")
			throw new SuException("cannot create instances of Database");
		if (method == "Connections")
			return Connections(args);
		if (method == "CurrentSize")
			return CurrentSize(args);
		if (method == "Cursors")
			return Cursors(args);
		if (method == "SessionId")
			return SessionId(args);
		if (method == "TempDest")
			return 0; // not relevant to jSuneido
		if (method == "Transactions")
			return Transactions(args);
		return super.invoke(self, method, args);
	}

	public static SuContainer Connections(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return theDbms.connections();
	}

	private Object CurrentSize(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		// need BigDecimal to handle long values
		return BigDecimal.valueOf(theDbms.size());
	}

	private int Cursors(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return theDbms.cursors();
	}

	public static final FunctionSpec stringFS =
			new FunctionSpec(array("string"), "");

	private Object SessionId(Object[] args) {
		args = Args.massage(stringFS, args);
		return theDbms.sessionid(Ops.toStr(args[0]));
	}

	private Object Transactions(Object[] args) {
		return new SuContainer(theDbms.tranlist());
	}

}
