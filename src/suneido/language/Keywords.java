package suneido.language;

import static suneido.language.Token.*;

import java.util.HashMap;
import java.util.Map;

public class Keywords {
	static final Map<String, Token> keywords = new HashMap<String, Token>(48);
	static {
		keywords.put("and", AND);
		keywords.put("bool", BOOL);
		keywords.put("break", BREAK);
		keywords.put("buffer", BUFFER);
		keywords.put("callback", CALLBACK);
		keywords.put("case", CASE);
		keywords.put("catch", CATCH);
		keywords.put("char", CHAR);
		keywords.put("class", CLASS);
		keywords.put("continue", CONTINUE);
		keywords.put("default", DEFAULT);
		keywords.put("dll", DLL);
		keywords.put("do", DO);
		keywords.put("double", DOUBLE);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("float", FLOAT);
		keywords.put("for", FOR);
		keywords.put("foreach", FOREACH);
		keywords.put("forever", FOREVER);
		keywords.put("function", FUNCTION);
		keywords.put("gdiobj", GDIOBJ);
		keywords.put("handle", HANDLE);
		keywords.put("if", IF);
		keywords.put("in", IN);
		keywords.put("int64", INT64);
		keywords.put("is", IS);
		keywords.put("isnt", ISNT);
		keywords.put("list", LIST);
		keywords.put("long", LONG);
		keywords.put("new", NEW);
		keywords.put("not", NOT);
		keywords.put("or", OR);
		keywords.put("resource", RESOURCE);
		keywords.put("return", RETURN);
		keywords.put("short", SHORT);
		keywords.put("string", STRING_KEYWORD);
		keywords.put("struct", STRUCT);
		keywords.put("super", SUPER);
		keywords.put("switch", SWITCH);
		keywords.put("this", THIS);
		keywords.put("throw", THROW);
		keywords.put("true", TRUE);
		keywords.put("try", TRY);
		keywords.put("value", VALUE);
		keywords.put("void", VOID);
		keywords.put("while", WHILE);
		keywords.put("xor", ISNT);
	};

	public static Token lookup(String s) {
		return keywords.get(s);
	}

}
