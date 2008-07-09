// $ANTLR 3.0.1 C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g 2008-07-09 10:49:33

package suneido.database.query;
import java.util.Collections;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class RequestParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "COLUMNS", "CREATE", "ID", "ENSURE", "ALTER", "RENAME", "DROP", "DELETE", "KEY", "INDEX", "UNIQUE", "LOWER", "IN", "CASCADE", "UPDATES", "TO", "WS", "COMMENT", "'('", "','", "')'", "'to'"
    };
    public static final int UNIQUE=14;
    public static final int ALTER=8;
    public static final int UPDATES=18;
    public static final int TO=19;
    public static final int KEY=12;
    public static final int DELETE=11;
    public static final int ID=6;
    public static final int CASCADE=17;
    public static final int EOF=-1;
    public static final int COLUMNS=4;
    public static final int INDEX=13;
    public static final int CREATE=5;
    public static final int WS=20;
    public static final int IN=16;
    public static final int DROP=10;
    public static final int ENSURE=7;
    public static final int LOWER=15;
    public static final int COMMENT=21;
    public static final int RENAME=9;

        public RequestParser(TokenStream input) {
            super(input);
        }
        

    public String[] getTokenNames() { return tokenNames; }
    public String getGrammarFileName() { return "C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g"; }


    public interface IRequest {
    	void create(String table, Schema schema);
    	void ensure(String table, Schema schema);
    	void alter_create(String table, Schema schema);
    	void alter_delete(String table, Schema schema);
    	void alter_rename(String table, List<Rename> renames);
    	void rename(String from, String to);
    	void drop(String table);
    }
    static class PrintRequest implements IRequest {
    	public void create(String table, Schema schema) {
    		System.out.println("addTable(" + table + ")");
    		schema(schema);
    	}
    	public void ensure(String table, Schema schema) {
    		System.out.println("ensure " + table);
    		schema(schema);
    	}
    	public void alter_create(String table, Schema schema) {
    		System.out.println("alter create " + table);
    		schema(schema);
    	}
    	public void alter_delete(String table, Schema schema) {
    		System.out.println("alter delete " + table);
    		schema(schema);
    	}
    	private void schema(Schema schema) {
    		for (String col : schema.columns)
    			System.out.println("addColumn(" + col + ")");
    		for (Index index : schema.indexes) {
    			System.out.print("addIndex(" + index.columns + ", " + index.isKey + index.isUnique);
    			if (index.in != null)
    				System.out.print(", " + index.in.table + ", " + index.in.columns + ", " + index.in.mode);
    			System.out.println(")");
    		}
    	}
    	public void alter_rename(String table, List<Rename> renames) {
    		for (Rename r : renames)
    			System.out.println("renameColumn(" + table + ", " + r.from + ", " + r.to + ")");
    	}
    	public void rename(String from, String to) {
    		System.out.println("renameTable(" + from + ", " + to + ")");
    	}
    	public void drop(String table) {
    		System.out.println("removeTable(" + table + ")");
    	}
    }
    public IRequest iRequest = new PrintRequest();

    static class Schema {
    	List<String> columns = Collections.EMPTY_LIST;
    	List<Index> indexes = new ArrayList<Index>();
    }
    static class Index {
    	boolean isKey = false;
    	boolean isUnique = false;
    	List<String> columns;
    	In in = In.nil;
    	Index(boolean isKey, boolean isUnique, List<String> columns, In in) {
    		this.isKey = isKey;
    		this.isUnique = isUnique;
    		this.columns = columns;
    		if (in != null)
    			this.in = in;
    	}
    }
    static class In {
    	static final In nil = new In(null, Collections.EMPTY_LIST, 0);
    	String table;
    	List<String> columns;
    	int mode;
    	In(String table, List<String> columns, int mode) {
    		this.table = table;
    		this.columns = columns;
    		this.mode = mode;
    	}
    }
    static class Rename {
    	String from;
    	String to;
    	Rename(String from, String to) {
    		this.from = from;
    		this.to = to;
    	}
    }


    protected static class request_scope {
        Schema schema;
    }
    protected Stack request_stack = new Stack();


    // $ANTLR start request
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:104:1: request : ( CREATE ID schema | ENSURE ID partial | ALTER ID RENAME renames | ALTER ID CREATE partial | ALTER ID ( DROP | DELETE ) partial | rename | ( DROP | DELETE ) ID );
    public final void request() throws RecognitionException {
        request_stack.push(new request_scope());
        Token ID1=null;
        Token ID2=null;
        Token ID3=null;
        Token ID5=null;
        Token ID6=null;
        Token ID8=null;
        List<Rename> renames4 = null;

        rename_return rename7 = null;


         ((request_scope)request_stack.peek()).schema = new Schema(); 
        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:107:2: ( CREATE ID schema | ENSURE ID partial | ALTER ID RENAME renames | ALTER ID CREATE partial | ALTER ID ( DROP | DELETE ) partial | rename | ( DROP | DELETE ) ID )
            int alt1=7;
            switch ( input.LA(1) ) {
            case CREATE:
                {
                alt1=1;
                }
                break;
            case ENSURE:
                {
                alt1=2;
                }
                break;
            case ALTER:
                {
                int LA1_3 = input.LA(2);

                if ( (LA1_3==ID) ) {
                    switch ( input.LA(3) ) {
                    case CREATE:
                        {
                        alt1=4;
                        }
                        break;
                    case DROP:
                    case DELETE:
                        {
                        alt1=5;
                        }
                        break;
                    case RENAME:
                        {
                        alt1=3;
                        }
                        break;
                    default:
                        NoViableAltException nvae =
                            new NoViableAltException("104:1: request : ( CREATE ID schema | ENSURE ID partial | ALTER ID RENAME renames | ALTER ID CREATE partial | ALTER ID ( DROP | DELETE ) partial | rename | ( DROP | DELETE ) ID );", 1, 6, input);

                        throw nvae;
                    }

                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("104:1: request : ( CREATE ID schema | ENSURE ID partial | ALTER ID RENAME renames | ALTER ID CREATE partial | ALTER ID ( DROP | DELETE ) partial | rename | ( DROP | DELETE ) ID );", 1, 3, input);

                    throw nvae;
                }
                }
                break;
            case RENAME:
                {
                alt1=6;
                }
                break;
            case DROP:
            case DELETE:
                {
                alt1=7;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("104:1: request : ( CREATE ID schema | ENSURE ID partial | ALTER ID RENAME renames | ALTER ID CREATE partial | ALTER ID ( DROP | DELETE ) partial | rename | ( DROP | DELETE ) ID );", 1, 0, input);

                throw nvae;
            }

            switch (alt1) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:107:4: CREATE ID schema
                    {
                    match(input,CREATE,FOLLOW_CREATE_in_request64); 
                    ID1=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_request66); 
                    pushFollow(FOLLOW_schema_in_request68);
                    schema();
                    _fsp--;

                     iRequest.create(ID1.getText(), ((request_scope)request_stack.peek()).schema); 

                    }
                    break;
                case 2 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:109:4: ENSURE ID partial
                    {
                    match(input,ENSURE,FOLLOW_ENSURE_in_request77); 
                    ID2=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_request79); 
                    pushFollow(FOLLOW_partial_in_request81);
                    partial();
                    _fsp--;

                     iRequest.ensure(ID2.getText(), ((request_scope)request_stack.peek()).schema); 

                    }
                    break;
                case 3 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:111:4: ALTER ID RENAME renames
                    {
                    match(input,ALTER,FOLLOW_ALTER_in_request90); 
                    ID3=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_request92); 
                    match(input,RENAME,FOLLOW_RENAME_in_request94); 
                    pushFollow(FOLLOW_renames_in_request96);
                    renames4=renames();
                    _fsp--;

                     iRequest.alter_rename(ID3.getText(), renames4); 

                    }
                    break;
                case 4 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:113:7: ALTER ID CREATE partial
                    {
                    match(input,ALTER,FOLLOW_ALTER_in_request108); 
                    ID5=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_request110); 
                    match(input,CREATE,FOLLOW_CREATE_in_request112); 
                    pushFollow(FOLLOW_partial_in_request114);
                    partial();
                    _fsp--;

                     iRequest.alter_create(ID5.getText(), ((request_scope)request_stack.peek()).schema); 

                    }
                    break;
                case 5 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:115:7: ALTER ID ( DROP | DELETE ) partial
                    {
                    match(input,ALTER,FOLLOW_ALTER_in_request129); 
                    ID6=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_request131); 
                    if ( (input.LA(1)>=DROP && input.LA(1)<=DELETE) ) {
                        input.consume();
                        errorRecovery=false;
                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recoverFromMismatchedSet(input,mse,FOLLOW_set_in_request133);    throw mse;
                    }

                    pushFollow(FOLLOW_partial_in_request139);
                    partial();
                    _fsp--;

                     iRequest.alter_delete(ID6.getText(), ((request_scope)request_stack.peek()).schema); 

                    }
                    break;
                case 6 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:117:7: rename
                    {
                    pushFollow(FOLLOW_rename_in_request154);
                    rename7=rename();
                    _fsp--;

                     iRequest.rename(rename7.from, rename7.to); 

                    }
                    break;
                case 7 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:119:7: ( DROP | DELETE ) ID
                    {
                    if ( (input.LA(1)>=DROP && input.LA(1)<=DELETE) ) {
                        input.consume();
                        errorRecovery=false;
                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recoverFromMismatchedSet(input,mse,FOLLOW_set_in_request169);    throw mse;
                    }

                    ID8=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_request175); 
                     iRequest.drop(ID8.getText()); 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            request_stack.pop();
        }
        return ;
    }
    // $ANTLR end request


    // $ANTLR start schema
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:123:1: schema : schema_columns ( key | index )* ;
    public final void schema() throws RecognitionException {
        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:123:8: ( schema_columns ( key | index )* )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:123:10: schema_columns ( key | index )*
            {
            pushFollow(FOLLOW_schema_columns_in_schema197);
            schema_columns();
            _fsp--;

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:123:25: ( key | index )*
            loop2:
            do {
                int alt2=3;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==KEY) ) {
                    alt2=1;
                }
                else if ( (LA2_0==INDEX) ) {
                    alt2=2;
                }


                switch (alt2) {
            	case 1 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:123:26: key
            	    {
            	    pushFollow(FOLLOW_key_in_schema200);
            	    key();
            	    _fsp--;


            	    }
            	    break;
            	case 2 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:123:30: index
            	    {
            	    pushFollow(FOLLOW_index_in_schema202);
            	    index();
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end schema


    // $ANTLR start partial
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:1: partial : ( schema_columns )? ( key | index )* ;
    public final void partial() throws RecognitionException {
        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:9: ( ( schema_columns )? ( key | index )* )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:11: ( schema_columns )? ( key | index )*
            {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:11: ( schema_columns )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==22) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:11: schema_columns
                    {
                    pushFollow(FOLLOW_schema_columns_in_partial213);
                    schema_columns();
                    _fsp--;


                    }
                    break;

            }

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:27: ( key | index )*
            loop4:
            do {
                int alt4=3;
                int LA4_0 = input.LA(1);

                if ( (LA4_0==KEY) ) {
                    alt4=1;
                }
                else if ( (LA4_0==INDEX) ) {
                    alt4=2;
                }


                switch (alt4) {
            	case 1 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:28: key
            	    {
            	    pushFollow(FOLLOW_key_in_partial217);
            	    key();
            	    _fsp--;


            	    }
            	    break;
            	case 2 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:125:32: index
            	    {
            	    pushFollow(FOLLOW_index_in_partial219);
            	    index();
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end partial


    // $ANTLR start schema_columns
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:127:1: schema_columns : columns ;
    public final void schema_columns() throws RecognitionException {
        List<String> columns9 = null;


        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:127:16: ( columns )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:127:18: columns
            {
            pushFollow(FOLLOW_columns_in_schema_columns229);
            columns9=columns();
            _fsp--;

             ((request_scope)request_stack.peek()).schema.columns = columns9; 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end schema_columns


    // $ANTLR start columns
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:131:1: columns returns [List<String> list] : '(' column[list] ( ( ',' )? column[list] )* ')' ;
    public final List<String> columns() throws RecognitionException {
        List<String> list = null;

         list = new ArrayList<String>(); 
        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:133:2: ( '(' column[list] ( ( ',' )? column[list] )* ')' )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:133:4: '(' column[list] ( ( ',' )? column[list] )* ')'
            {
            match(input,22,FOLLOW_22_in_columns253); 
            pushFollow(FOLLOW_column_in_columns255);
            column(list);
            _fsp--;

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:133:21: ( ( ',' )? column[list] )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( (LA6_0==ID||LA6_0==23) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:133:22: ( ',' )? column[list]
            	    {
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:133:22: ( ',' )?
            	    int alt5=2;
            	    int LA5_0 = input.LA(1);

            	    if ( (LA5_0==23) ) {
            	        alt5=1;
            	    }
            	    switch (alt5) {
            	        case 1 :
            	            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:133:22: ','
            	            {
            	            match(input,23,FOLLOW_23_in_columns259); 

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_column_in_columns262);
            	    column(list);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            match(input,24,FOLLOW_24_in_columns268); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return list;
    }
    // $ANTLR end columns


    // $ANTLR start column
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:134:1: column[List<String> list] : ID ;
    public final void column(List<String> list) throws RecognitionException {
        Token ID10=null;

        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:135:2: ( ID )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:135:4: ID
            {
            ID10=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_column278); 
             list.add(ID10.getText()); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end column


    // $ANTLR start key
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:137:1: key : KEY columns ( in )? ;
    public final void key() throws RecognitionException {
        List<String> columns11 = null;

        In in12 = null;


        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:137:5: ( KEY columns ( in )? )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:137:7: KEY columns ( in )?
            {
            match(input,KEY,FOLLOW_KEY_in_key290); 
            pushFollow(FOLLOW_columns_in_key292);
            columns11=columns();
            _fsp--;

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:137:19: ( in )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==IN) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:137:19: in
                    {
                    pushFollow(FOLLOW_in_in_key294);
                    in12=in();
                    _fsp--;


                    }
                    break;

            }

             ((request_scope)request_stack.peek()).schema.indexes.add(new Index(true, false, columns11, in12)); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end key


    // $ANTLR start index
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:1: index : INDEX (u= UNIQUE | LOWER )? columns ( in )? ;
    public final void index() throws RecognitionException {
        Token u=null;
        List<String> columns13 = null;

        In in14 = null;


        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:7: ( INDEX (u= UNIQUE | LOWER )? columns ( in )? )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:9: INDEX (u= UNIQUE | LOWER )? columns ( in )?
            {
            match(input,INDEX,FOLLOW_INDEX_in_index310); 
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:15: (u= UNIQUE | LOWER )?
            int alt8=3;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==UNIQUE) ) {
                alt8=1;
            }
            else if ( (LA8_0==LOWER) ) {
                alt8=2;
            }
            switch (alt8) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:16: u= UNIQUE
                    {
                    u=(Token)input.LT(1);
                    match(input,UNIQUE,FOLLOW_UNIQUE_in_index315); 

                    }
                    break;
                case 2 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:27: LOWER
                    {
                    match(input,LOWER,FOLLOW_LOWER_in_index319); 

                    }
                    break;

            }

            pushFollow(FOLLOW_columns_in_index323);
            columns13=columns();
            _fsp--;

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:43: ( in )?
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==IN) ) {
                alt9=1;
            }
            switch (alt9) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:141:43: in
                    {
                    pushFollow(FOLLOW_in_in_index325);
                    in14=in();
                    _fsp--;


                    }
                    break;

            }

             ((request_scope)request_stack.peek()).schema.indexes.add(new Index(false, u != null, columns13, in14)); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end index


    // $ANTLR start in
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:145:1: in returns [In in] : IN ID ( columns )? (c= CASCADE (u= UPDATES )? )? ;
    public final In in() throws RecognitionException {
        In in = null;

        Token c=null;
        Token u=null;
        Token ID15=null;
        List<String> columns16 = null;


        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:2: ( IN ID ( columns )? (c= CASCADE (u= UPDATES )? )? )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:4: IN ID ( columns )? (c= CASCADE (u= UPDATES )? )?
            {
            match(input,IN,FOLLOW_IN_in_in346); 
            ID15=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_in348); 
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:10: ( columns )?
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==22) ) {
                alt10=1;
            }
            switch (alt10) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:10: columns
                    {
                    pushFollow(FOLLOW_columns_in_in350);
                    columns16=columns();
                    _fsp--;


                    }
                    break;

            }

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:19: (c= CASCADE (u= UPDATES )? )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==CASCADE) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:20: c= CASCADE (u= UPDATES )?
                    {
                    c=(Token)input.LT(1);
                    match(input,CASCADE,FOLLOW_CASCADE_in_in356); 
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:31: (u= UPDATES )?
                    int alt11=2;
                    int LA11_0 = input.LA(1);

                    if ( (LA11_0==UPDATES) ) {
                        alt11=1;
                    }
                    switch (alt11) {
                        case 1 :
                            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:146:31: u= UPDATES
                            {
                            u=(Token)input.LT(1);
                            match(input,UPDATES,FOLLOW_UPDATES_in_in360); 

                            }
                            break;

                    }


                    }
                    break;

            }


            		int mode = 0;
            		if (u != null)
            			mode = 1;
            		else if (c != null)
            			mode = 3;
            		in = new In(ID15.getText(), columns16, mode);
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return in;
    }
    // $ANTLR end in


    // $ANTLR start renames
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:157:1: renames returns [List<Rename> list] : rename1[list] ( ',' rename1[list] )* ;
    public final List<Rename> renames() throws RecognitionException {
        List<Rename> list = null;

         list = new ArrayList<Rename>(); 
        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:159:2: ( rename1[list] ( ',' rename1[list] )* )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:159:4: rename1[list] ( ',' rename1[list] )*
            {
            pushFollow(FOLLOW_rename1_in_renames390);
            rename1(list);
            _fsp--;

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:159:18: ( ',' rename1[list] )*
            loop13:
            do {
                int alt13=2;
                int LA13_0 = input.LA(1);

                if ( (LA13_0==23) ) {
                    alt13=1;
                }


                switch (alt13) {
            	case 1 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:159:19: ',' rename1[list]
            	    {
            	    match(input,23,FOLLOW_23_in_renames394); 
            	    pushFollow(FOLLOW_rename1_in_renames396);
            	    rename1(list);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    break loop13;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return list;
    }
    // $ANTLR end renames


    // $ANTLR start rename1
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:160:1: rename1[List<Rename> list] : f= ID 'to' t= ID ;
    public final void rename1(List<Rename> list) throws RecognitionException {
        Token f=null;
        Token t=null;

        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:161:2: (f= ID 'to' t= ID )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:161:4: f= ID 'to' t= ID
            {
            f=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_rename1412); 
            match(input,25,FOLLOW_25_in_rename1414); 
            t=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_rename1418); 
             list.add(new Rename(f.getText(), t.getText())); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end rename1

    public static class rename_return extends ParserRuleReturnScope {
        public String from;
        public String to;
    };

    // $ANTLR start rename
    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:165:1: rename returns [String from, String to] : RENAME f= ID TO t= ID ;
    public final rename_return rename() throws RecognitionException {
        rename_return retval = new rename_return();
        retval.start = input.LT(1);

        Token f=null;
        Token t=null;

        try {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:166:2: ( RENAME f= ID TO t= ID )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:166:4: RENAME f= ID TO t= ID
            {
            match(input,RENAME,FOLLOW_RENAME_in_rename444); 
            f=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_rename448); 
            match(input,TO,FOLLOW_TO_in_rename450); 
            t=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_rename454); 
             retval.from = f.getText(); retval.to = t.getText(); 

            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end rename


 

    public static final BitSet FOLLOW_CREATE_in_request64 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_request66 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_schema_in_request68 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ENSURE_in_request77 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_request79 = new BitSet(new long[]{0x0000000000403002L});
    public static final BitSet FOLLOW_partial_in_request81 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALTER_in_request90 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_request92 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_RENAME_in_request94 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_renames_in_request96 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALTER_in_request108 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_request110 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_CREATE_in_request112 = new BitSet(new long[]{0x0000000000403002L});
    public static final BitSet FOLLOW_partial_in_request114 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALTER_in_request129 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_request131 = new BitSet(new long[]{0x0000000000000C00L});
    public static final BitSet FOLLOW_set_in_request133 = new BitSet(new long[]{0x0000000000403002L});
    public static final BitSet FOLLOW_partial_in_request139 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rename_in_request154 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_request169 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_request175 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_schema_columns_in_schema197 = new BitSet(new long[]{0x0000000000003002L});
    public static final BitSet FOLLOW_key_in_schema200 = new BitSet(new long[]{0x0000000000003002L});
    public static final BitSet FOLLOW_index_in_schema202 = new BitSet(new long[]{0x0000000000003002L});
    public static final BitSet FOLLOW_schema_columns_in_partial213 = new BitSet(new long[]{0x0000000000003002L});
    public static final BitSet FOLLOW_key_in_partial217 = new BitSet(new long[]{0x0000000000003002L});
    public static final BitSet FOLLOW_index_in_partial219 = new BitSet(new long[]{0x0000000000003002L});
    public static final BitSet FOLLOW_columns_in_schema_columns229 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_columns253 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_column_in_columns255 = new BitSet(new long[]{0x0000000001800040L});
    public static final BitSet FOLLOW_23_in_columns259 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_column_in_columns262 = new BitSet(new long[]{0x0000000001800040L});
    public static final BitSet FOLLOW_24_in_columns268 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_column278 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_KEY_in_key290 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_columns_in_key292 = new BitSet(new long[]{0x0000000000010002L});
    public static final BitSet FOLLOW_in_in_key294 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INDEX_in_index310 = new BitSet(new long[]{0x000000000040C000L});
    public static final BitSet FOLLOW_UNIQUE_in_index315 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_LOWER_in_index319 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_columns_in_index323 = new BitSet(new long[]{0x0000000000010002L});
    public static final BitSet FOLLOW_in_in_index325 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IN_in_in346 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_in348 = new BitSet(new long[]{0x0000000000420002L});
    public static final BitSet FOLLOW_columns_in_in350 = new BitSet(new long[]{0x0000000000020002L});
    public static final BitSet FOLLOW_CASCADE_in_in356 = new BitSet(new long[]{0x0000000000040002L});
    public static final BitSet FOLLOW_UPDATES_in_in360 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rename1_in_renames390 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_23_in_renames394 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_rename1_in_renames396 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_ID_in_rename1412 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_rename1414 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_rename1418 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RENAME_in_rename444 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_rename448 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_TO_in_rename450 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_ID_in_rename454 = new BitSet(new long[]{0x0000000000000002L});

}