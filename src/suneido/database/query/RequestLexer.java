// $ANTLR 3.0.1 C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g 2008-07-09 10:49:34
 package suneido.database.query; 

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class RequestLexer extends Lexer {
    public static final int UNIQUE=14;
    public static final int ALTER=8;
    public static final int UPDATES=18;
    public static final int TO=19;
    public static final int KEY=12;
    public static final int DELETE=11;
    public static final int T25=25;
    public static final int ID=6;
    public static final int CASCADE=17;
    public static final int Tokens=26;
    public static final int T24=24;
    public static final int EOF=-1;
    public static final int T23=23;
    public static final int T22=22;
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
    public RequestLexer() {;} 
    public RequestLexer(CharStream input) {
        super(input);
    }
    public String getGrammarFileName() { return "C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g"; }

    // $ANTLR start T22
    public final void mT22() throws RecognitionException {
        try {
            int _type = T22;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:8:5: ( '(' )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:8:7: '('
            {
            match('('); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T22

    // $ANTLR start T23
    public final void mT23() throws RecognitionException {
        try {
            int _type = T23;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:9:5: ( ',' )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:9:7: ','
            {
            match(','); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T23

    // $ANTLR start T24
    public final void mT24() throws RecognitionException {
        try {
            int _type = T24;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:10:5: ( ')' )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:10:7: ')'
            {
            match(')'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T24

    // $ANTLR start T25
    public final void mT25() throws RecognitionException {
        try {
            int _type = T25;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:11:5: ( 'to' )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:11:7: 'to'
            {
            match("to"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T25

    // $ANTLR start CREATE
    public final void mCREATE() throws RecognitionException {
        try {
            int _type = CREATE;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:170:8: ( ( 'c' | 'C' ) ( 'r' | 'R' ) ( 'e' | 'E' ) ( 'a' | 'A' ) ( 't' | 'T' ) ( 'e' | 'E' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:170:10: ( 'c' | 'C' ) ( 'r' | 'R' ) ( 'e' | 'E' ) ( 'a' | 'A' ) ( 't' | 'T' ) ( 'e' | 'E' )
            {
            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end CREATE

    // $ANTLR start ENSURE
    public final void mENSURE() throws RecognitionException {
        try {
            int _type = ENSURE;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:171:8: ( ( 'e' | 'E' ) ( 'n' | 'N' ) ( 's' | 'S' ) ( 'u' | 'U' ) ( 'r' | 'R' ) ( 'e' | 'E' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:171:10: ( 'e' | 'E' ) ( 'n' | 'N' ) ( 's' | 'S' ) ( 'u' | 'U' ) ( 'r' | 'R' ) ( 'e' | 'E' )
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='U'||input.LA(1)=='u' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ENSURE

    // $ANTLR start DELETE
    public final void mDELETE() throws RecognitionException {
        try {
            int _type = DELETE;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:172:8: ( ( 'd' | 'D' ) ( 'e' | 'E' ) ( 'l' | 'L' ) ( 'e' | 'E' ) ( 't' | 'T' ) ( 'e' | 'E' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:172:10: ( 'd' | 'D' ) ( 'e' | 'E' ) ( 'l' | 'L' ) ( 'e' | 'E' ) ( 't' | 'T' ) ( 'e' | 'E' )
            {
            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DELETE

    // $ANTLR start DROP
    public final void mDROP() throws RecognitionException {
        try {
            int _type = DROP;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:173:6: ( ( 'd' | 'D' ) ( 'r' | 'R' ) ( 'o' | 'O' ) ( 'p' | 'P' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:173:8: ( 'd' | 'D' ) ( 'r' | 'R' ) ( 'o' | 'O' ) ( 'p' | 'P' )
            {
            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='P'||input.LA(1)=='p' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DROP

    // $ANTLR start ALTER
    public final void mALTER() throws RecognitionException {
        try {
            int _type = ALTER;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:174:7: ( ( 'a' | 'A' ) ( 'l' | 'L' ) ( 't' | 'T' ) ( 'e' | 'E' ) ( 'r' | 'R' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:174:9: ( 'a' | 'A' ) ( 'l' | 'L' ) ( 't' | 'T' ) ( 'e' | 'E' ) ( 'r' | 'R' )
            {
            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ALTER

    // $ANTLR start RENAME
    public final void mRENAME() throws RecognitionException {
        try {
            int _type = RENAME;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:175:8: ( ( 'r' | 'R' ) ( 'e' | 'E' ) ( 'n' | 'N' ) ( 'a' | 'A' ) ( 'm' | 'M' ) ( 'e' | 'E' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:175:10: ( 'r' | 'R' ) ( 'e' | 'E' ) ( 'n' | 'N' ) ( 'a' | 'A' ) ( 'm' | 'M' ) ( 'e' | 'E' )
            {
            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='M'||input.LA(1)=='m' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RENAME

    // $ANTLR start IN
    public final void mIN() throws RecognitionException {
        try {
            int _type = IN;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:176:5: ( ( 'i' | 'I' ) ( 'n' | 'N' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:176:7: ( 'i' | 'I' ) ( 'n' | 'N' )
            {
            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end IN

    // $ANTLR start TO
    public final void mTO() throws RecognitionException {
        try {
            int _type = TO;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:177:5: ( ( 't' | 'T' ) ( 'o' | 'O' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:177:7: ( 't' | 'T' ) ( 'o' | 'O' )
            {
            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end TO

    // $ANTLR start UNIQUE
    public final void mUNIQUE() throws RecognitionException {
        try {
            int _type = UNIQUE;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:178:8: ( ( 'u' | 'U' ) ( 'n' | 'N' ) ( 'i' | 'I' ) ( 'q' | 'Q' ) ( 'u' | 'U' ) ( 'e' | 'E' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:178:10: ( 'u' | 'U' ) ( 'n' | 'N' ) ( 'i' | 'I' ) ( 'q' | 'Q' ) ( 'u' | 'U' ) ( 'e' | 'E' )
            {
            if ( input.LA(1)=='U'||input.LA(1)=='u' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='Q'||input.LA(1)=='q' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='U'||input.LA(1)=='u' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end UNIQUE

    // $ANTLR start LOWER
    public final void mLOWER() throws RecognitionException {
        try {
            int _type = LOWER;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:179:7: ( ( 'l' | 'L' ) ( 'o' | 'O' ) ( 'w' | 'W' ) ( 'e' | 'E' ) ( 'r' | 'R' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:179:9: ( 'l' | 'L' ) ( 'o' | 'O' ) ( 'w' | 'W' ) ( 'e' | 'E' ) ( 'r' | 'R' )
            {
            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='W'||input.LA(1)=='w' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LOWER

    // $ANTLR start CASCADE
    public final void mCASCADE() throws RecognitionException {
        try {
            int _type = CASCADE;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:180:9: ( ( 'c' | 'C' ) ( 'a' | 'A' ) ( 's' | 'S' ) ( 'c' | 'C' ) ( 'a' | 'A' ) ( 'd' | 'D' ) ( 'e' | 'E' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:180:11: ( 'c' | 'C' ) ( 'a' | 'A' ) ( 's' | 'S' ) ( 'c' | 'C' ) ( 'a' | 'A' ) ( 'd' | 'D' ) ( 'e' | 'E' )
            {
            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end CASCADE

    // $ANTLR start UPDATES
    public final void mUPDATES() throws RecognitionException {
        try {
            int _type = UPDATES;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:181:9: ( ( 'u' | 'U' ) ( 'p' | 'P' ) ( 'd' | 'D' ) ( 'a' | 'A' ) ( 't' | 'T' ) ( 'e' | 'E' ) ( 's' | 'S' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:181:11: ( 'u' | 'U' ) ( 'p' | 'P' ) ( 'd' | 'D' ) ( 'a' | 'A' ) ( 't' | 'T' ) ( 'e' | 'E' ) ( 's' | 'S' )
            {
            if ( input.LA(1)=='U'||input.LA(1)=='u' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='P'||input.LA(1)=='p' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end UPDATES

    // $ANTLR start INDEX
    public final void mINDEX() throws RecognitionException {
        try {
            int _type = INDEX;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:182:7: ( ( 'i' | 'I' ) ( 'n' | 'N' ) ( 'd' | 'D' ) ( 'e' | 'E' ) ( 'x' | 'X' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:182:9: ( 'i' | 'I' ) ( 'n' | 'N' ) ( 'd' | 'D' ) ( 'e' | 'E' ) ( 'x' | 'X' )
            {
            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='X'||input.LA(1)=='x' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end INDEX

    // $ANTLR start KEY
    public final void mKEY() throws RecognitionException {
        try {
            int _type = KEY;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:183:6: ( ( 'k' | 'K' ) ( 'e' | 'E' ) ( 'y' | 'Y' ) )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:183:8: ( 'k' | 'K' ) ( 'e' | 'E' ) ( 'y' | 'Y' )
            {
            if ( input.LA(1)=='K'||input.LA(1)=='k' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            if ( input.LA(1)=='Y'||input.LA(1)=='y' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end KEY

    // $ANTLR start ID
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:185:7: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' )* ( '?' | '!' )? )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:185:9: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' )* ( '?' | '!' )?
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:185:32: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='0' && LA1_0<='9')||(LA1_0>='A' && LA1_0<='Z')||LA1_0=='_'||(LA1_0>='a' && LA1_0<='z')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:185:65: ( '?' | '!' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='!'||LA2_0=='?') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:
                    {
                    if ( input.LA(1)=='!'||input.LA(1)=='?' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recover(mse);    throw mse;
                    }


                    }
                    break;

            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ID

    // $ANTLR start WS
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:187:7: ( ( ' ' | '\\t' | '\\r' | '\\n' )+ )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:187:9: ( ' ' | '\\t' | '\\r' | '\\n' )+
            {
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:187:9: ( ' ' | '\\t' | '\\r' | '\\n' )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='\t' && LA3_0<='\n')||LA3_0=='\r'||LA3_0==' ') ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);

             channel = HIDDEN; 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end WS

    // $ANTLR start COMMENT
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:188:9: ( '/*' ( . )* '*/' )
            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:188:11: '/*' ( . )* '*/'
            {
            match("/*"); 

            // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:188:16: ( . )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0=='*') ) {
                    int LA4_1 = input.LA(2);

                    if ( (LA4_1=='/') ) {
                        alt4=2;
                    }
                    else if ( ((LA4_1>='\u0000' && LA4_1<='.')||(LA4_1>='0' && LA4_1<='\uFFFE')) ) {
                        alt4=1;
                    }


                }
                else if ( ((LA4_0>='\u0000' && LA4_0<=')')||(LA4_0>='+' && LA4_0<='\uFFFE')) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:188:16: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);

            match("*/"); 

             channel = HIDDEN; 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end COMMENT

    public void mTokens() throws RecognitionException {
        // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:8: ( T22 | T23 | T24 | T25 | CREATE | ENSURE | DELETE | DROP | ALTER | RENAME | IN | TO | UNIQUE | LOWER | CASCADE | UPDATES | INDEX | KEY | ID | WS | COMMENT )
        int alt5=21;
        switch ( input.LA(1) ) {
        case '(':
            {
            alt5=1;
            }
            break;
        case ',':
            {
            alt5=2;
            }
            break;
        case ')':
            {
            alt5=3;
            }
            break;
        case 't':
            {
            switch ( input.LA(2) ) {
            case 'o':
                {
                int LA5_18 = input.LA(3);

                if ( (LA5_18=='!'||(LA5_18>='0' && LA5_18<='9')||LA5_18=='?'||(LA5_18>='A' && LA5_18<='Z')||LA5_18=='_'||(LA5_18>='a' && LA5_18<='z')) ) {
                    alt5=19;
                }
                else {
                    alt5=4;}
                }
                break;
            case 'O':
                {
                int LA5_19 = input.LA(3);

                if ( (LA5_19=='!'||(LA5_19>='0' && LA5_19<='9')||LA5_19=='?'||(LA5_19>='A' && LA5_19<='Z')||LA5_19=='_'||(LA5_19>='a' && LA5_19<='z')) ) {
                    alt5=19;
                }
                else {
                    alt5=12;}
                }
                break;
            default:
                alt5=19;}

            }
            break;
        case 'C':
        case 'c':
            {
            switch ( input.LA(2) ) {
            case 'R':
            case 'r':
                {
                int LA5_20 = input.LA(3);

                if ( (LA5_20=='E'||LA5_20=='e') ) {
                    int LA5_34 = input.LA(4);

                    if ( (LA5_34=='A'||LA5_34=='a') ) {
                        int LA5_47 = input.LA(5);

                        if ( (LA5_47=='T'||LA5_47=='t') ) {
                            int LA5_59 = input.LA(6);

                            if ( (LA5_59=='E'||LA5_59=='e') ) {
                                int LA5_70 = input.LA(7);

                                if ( (LA5_70=='!'||(LA5_70>='0' && LA5_70<='9')||LA5_70=='?'||(LA5_70>='A' && LA5_70<='Z')||LA5_70=='_'||(LA5_70>='a' && LA5_70<='z')) ) {
                                    alt5=19;
                                }
                                else {
                                    alt5=5;}
                            }
                            else {
                                alt5=19;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
                }
                break;
            case 'A':
            case 'a':
                {
                int LA5_21 = input.LA(3);

                if ( (LA5_21=='S'||LA5_21=='s') ) {
                    int LA5_35 = input.LA(4);

                    if ( (LA5_35=='C'||LA5_35=='c') ) {
                        int LA5_48 = input.LA(5);

                        if ( (LA5_48=='A'||LA5_48=='a') ) {
                            int LA5_60 = input.LA(6);

                            if ( (LA5_60=='D'||LA5_60=='d') ) {
                                int LA5_71 = input.LA(7);

                                if ( (LA5_71=='E'||LA5_71=='e') ) {
                                    int LA5_81 = input.LA(8);

                                    if ( (LA5_81=='!'||(LA5_81>='0' && LA5_81<='9')||LA5_81=='?'||(LA5_81>='A' && LA5_81<='Z')||LA5_81=='_'||(LA5_81>='a' && LA5_81<='z')) ) {
                                        alt5=19;
                                    }
                                    else {
                                        alt5=15;}
                                }
                                else {
                                    alt5=19;}
                            }
                            else {
                                alt5=19;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
                }
                break;
            default:
                alt5=19;}

            }
            break;
        case 'E':
        case 'e':
            {
            int LA5_6 = input.LA(2);

            if ( (LA5_6=='N'||LA5_6=='n') ) {
                int LA5_22 = input.LA(3);

                if ( (LA5_22=='S'||LA5_22=='s') ) {
                    int LA5_36 = input.LA(4);

                    if ( (LA5_36=='U'||LA5_36=='u') ) {
                        int LA5_49 = input.LA(5);

                        if ( (LA5_49=='R'||LA5_49=='r') ) {
                            int LA5_61 = input.LA(6);

                            if ( (LA5_61=='E'||LA5_61=='e') ) {
                                int LA5_72 = input.LA(7);

                                if ( (LA5_72=='!'||(LA5_72>='0' && LA5_72<='9')||LA5_72=='?'||(LA5_72>='A' && LA5_72<='Z')||LA5_72=='_'||(LA5_72>='a' && LA5_72<='z')) ) {
                                    alt5=19;
                                }
                                else {
                                    alt5=6;}
                            }
                            else {
                                alt5=19;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
            }
            else {
                alt5=19;}
            }
            break;
        case 'D':
        case 'd':
            {
            switch ( input.LA(2) ) {
            case 'E':
            case 'e':
                {
                int LA5_23 = input.LA(3);

                if ( (LA5_23=='L'||LA5_23=='l') ) {
                    int LA5_37 = input.LA(4);

                    if ( (LA5_37=='E'||LA5_37=='e') ) {
                        int LA5_50 = input.LA(5);

                        if ( (LA5_50=='T'||LA5_50=='t') ) {
                            int LA5_62 = input.LA(6);

                            if ( (LA5_62=='E'||LA5_62=='e') ) {
                                int LA5_73 = input.LA(7);

                                if ( (LA5_73=='!'||(LA5_73>='0' && LA5_73<='9')||LA5_73=='?'||(LA5_73>='A' && LA5_73<='Z')||LA5_73=='_'||(LA5_73>='a' && LA5_73<='z')) ) {
                                    alt5=19;
                                }
                                else {
                                    alt5=7;}
                            }
                            else {
                                alt5=19;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
                }
                break;
            case 'R':
            case 'r':
                {
                int LA5_24 = input.LA(3);

                if ( (LA5_24=='O'||LA5_24=='o') ) {
                    int LA5_38 = input.LA(4);

                    if ( (LA5_38=='P'||LA5_38=='p') ) {
                        int LA5_51 = input.LA(5);

                        if ( (LA5_51=='!'||(LA5_51>='0' && LA5_51<='9')||LA5_51=='?'||(LA5_51>='A' && LA5_51<='Z')||LA5_51=='_'||(LA5_51>='a' && LA5_51<='z')) ) {
                            alt5=19;
                        }
                        else {
                            alt5=8;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
                }
                break;
            default:
                alt5=19;}

            }
            break;
        case 'A':
        case 'a':
            {
            int LA5_8 = input.LA(2);

            if ( (LA5_8=='L'||LA5_8=='l') ) {
                int LA5_25 = input.LA(3);

                if ( (LA5_25=='T'||LA5_25=='t') ) {
                    int LA5_39 = input.LA(4);

                    if ( (LA5_39=='E'||LA5_39=='e') ) {
                        int LA5_52 = input.LA(5);

                        if ( (LA5_52=='R'||LA5_52=='r') ) {
                            int LA5_64 = input.LA(6);

                            if ( (LA5_64=='!'||(LA5_64>='0' && LA5_64<='9')||LA5_64=='?'||(LA5_64>='A' && LA5_64<='Z')||LA5_64=='_'||(LA5_64>='a' && LA5_64<='z')) ) {
                                alt5=19;
                            }
                            else {
                                alt5=9;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
            }
            else {
                alt5=19;}
            }
            break;
        case 'R':
        case 'r':
            {
            int LA5_9 = input.LA(2);

            if ( (LA5_9=='E'||LA5_9=='e') ) {
                int LA5_26 = input.LA(3);

                if ( (LA5_26=='N'||LA5_26=='n') ) {
                    int LA5_40 = input.LA(4);

                    if ( (LA5_40=='A'||LA5_40=='a') ) {
                        int LA5_53 = input.LA(5);

                        if ( (LA5_53=='M'||LA5_53=='m') ) {
                            int LA5_65 = input.LA(6);

                            if ( (LA5_65=='E'||LA5_65=='e') ) {
                                int LA5_75 = input.LA(7);

                                if ( (LA5_75=='!'||(LA5_75>='0' && LA5_75<='9')||LA5_75=='?'||(LA5_75>='A' && LA5_75<='Z')||LA5_75=='_'||(LA5_75>='a' && LA5_75<='z')) ) {
                                    alt5=19;
                                }
                                else {
                                    alt5=10;}
                            }
                            else {
                                alt5=19;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
            }
            else {
                alt5=19;}
            }
            break;
        case 'I':
        case 'i':
            {
            int LA5_10 = input.LA(2);

            if ( (LA5_10=='N'||LA5_10=='n') ) {
                switch ( input.LA(3) ) {
                case 'D':
                case 'd':
                    {
                    int LA5_41 = input.LA(4);

                    if ( (LA5_41=='E'||LA5_41=='e') ) {
                        int LA5_54 = input.LA(5);

                        if ( (LA5_54=='X'||LA5_54=='x') ) {
                            int LA5_66 = input.LA(6);

                            if ( (LA5_66=='!'||(LA5_66>='0' && LA5_66<='9')||LA5_66=='?'||(LA5_66>='A' && LA5_66<='Z')||LA5_66=='_'||(LA5_66>='a' && LA5_66<='z')) ) {
                                alt5=19;
                            }
                            else {
                                alt5=17;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                    }
                    break;
                case '!':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '?':
                case 'A':
                case 'B':
                case 'C':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case '_':
                case 'a':
                case 'b':
                case 'c':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                    {
                    alt5=19;
                    }
                    break;
                default:
                    alt5=11;}

            }
            else {
                alt5=19;}
            }
            break;
        case 'T':
            {
            int LA5_11 = input.LA(2);

            if ( (LA5_11=='O'||LA5_11=='o') ) {
                int LA5_19 = input.LA(3);

                if ( (LA5_19=='!'||(LA5_19>='0' && LA5_19<='9')||LA5_19=='?'||(LA5_19>='A' && LA5_19<='Z')||LA5_19=='_'||(LA5_19>='a' && LA5_19<='z')) ) {
                    alt5=19;
                }
                else {
                    alt5=12;}
            }
            else {
                alt5=19;}
            }
            break;
        case 'U':
        case 'u':
            {
            switch ( input.LA(2) ) {
            case 'N':
            case 'n':
                {
                int LA5_28 = input.LA(3);

                if ( (LA5_28=='I'||LA5_28=='i') ) {
                    int LA5_43 = input.LA(4);

                    if ( (LA5_43=='Q'||LA5_43=='q') ) {
                        int LA5_55 = input.LA(5);

                        if ( (LA5_55=='U'||LA5_55=='u') ) {
                            int LA5_67 = input.LA(6);

                            if ( (LA5_67=='E'||LA5_67=='e') ) {
                                int LA5_77 = input.LA(7);

                                if ( (LA5_77=='!'||(LA5_77>='0' && LA5_77<='9')||LA5_77=='?'||(LA5_77>='A' && LA5_77<='Z')||LA5_77=='_'||(LA5_77>='a' && LA5_77<='z')) ) {
                                    alt5=19;
                                }
                                else {
                                    alt5=13;}
                            }
                            else {
                                alt5=19;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
                }
                break;
            case 'P':
            case 'p':
                {
                int LA5_29 = input.LA(3);

                if ( (LA5_29=='D'||LA5_29=='d') ) {
                    int LA5_44 = input.LA(4);

                    if ( (LA5_44=='A'||LA5_44=='a') ) {
                        int LA5_56 = input.LA(5);

                        if ( (LA5_56=='T'||LA5_56=='t') ) {
                            int LA5_68 = input.LA(6);

                            if ( (LA5_68=='E'||LA5_68=='e') ) {
                                int LA5_78 = input.LA(7);

                                if ( (LA5_78=='S'||LA5_78=='s') ) {
                                    int LA5_86 = input.LA(8);

                                    if ( (LA5_86=='!'||(LA5_86>='0' && LA5_86<='9')||LA5_86=='?'||(LA5_86>='A' && LA5_86<='Z')||LA5_86=='_'||(LA5_86>='a' && LA5_86<='z')) ) {
                                        alt5=19;
                                    }
                                    else {
                                        alt5=16;}
                                }
                                else {
                                    alt5=19;}
                            }
                            else {
                                alt5=19;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
                }
                break;
            default:
                alt5=19;}

            }
            break;
        case 'L':
        case 'l':
            {
            int LA5_13 = input.LA(2);

            if ( (LA5_13=='O'||LA5_13=='o') ) {
                int LA5_30 = input.LA(3);

                if ( (LA5_30=='W'||LA5_30=='w') ) {
                    int LA5_45 = input.LA(4);

                    if ( (LA5_45=='E'||LA5_45=='e') ) {
                        int LA5_57 = input.LA(5);

                        if ( (LA5_57=='R'||LA5_57=='r') ) {
                            int LA5_69 = input.LA(6);

                            if ( (LA5_69=='!'||(LA5_69>='0' && LA5_69<='9')||LA5_69=='?'||(LA5_69>='A' && LA5_69<='Z')||LA5_69=='_'||(LA5_69>='a' && LA5_69<='z')) ) {
                                alt5=19;
                            }
                            else {
                                alt5=14;}
                        }
                        else {
                            alt5=19;}
                    }
                    else {
                        alt5=19;}
                }
                else {
                    alt5=19;}
            }
            else {
                alt5=19;}
            }
            break;
        case 'K':
        case 'k':
            {
            int LA5_14 = input.LA(2);

            if ( (LA5_14=='E'||LA5_14=='e') ) {
                int LA5_31 = input.LA(3);

                if ( (LA5_31=='Y'||LA5_31=='y') ) {
                    int LA5_46 = input.LA(4);

                    if ( (LA5_46=='!'||(LA5_46>='0' && LA5_46<='9')||LA5_46=='?'||(LA5_46>='A' && LA5_46<='Z')||LA5_46=='_'||(LA5_46>='a' && LA5_46<='z')) ) {
                        alt5=19;
                    }
                    else {
                        alt5=18;}
                }
                else {
                    alt5=19;}
            }
            else {
                alt5=19;}
            }
            break;
        case 'B':
        case 'F':
        case 'G':
        case 'H':
        case 'J':
        case 'M':
        case 'N':
        case 'O':
        case 'P':
        case 'Q':
        case 'S':
        case 'V':
        case 'W':
        case 'X':
        case 'Y':
        case 'Z':
        case '_':
        case 'b':
        case 'f':
        case 'g':
        case 'h':
        case 'j':
        case 'm':
        case 'n':
        case 'o':
        case 'p':
        case 'q':
        case 's':
        case 'v':
        case 'w':
        case 'x':
        case 'y':
        case 'z':
            {
            alt5=19;
            }
            break;
        case '\t':
        case '\n':
        case '\r':
        case ' ':
            {
            alt5=20;
            }
            break;
        case '/':
            {
            alt5=21;
            }
            break;
        default:
            NoViableAltException nvae =
                new NoViableAltException("1:1: Tokens : ( T22 | T23 | T24 | T25 | CREATE | ENSURE | DELETE | DROP | ALTER | RENAME | IN | TO | UNIQUE | LOWER | CASCADE | UPDATES | INDEX | KEY | ID | WS | COMMENT );", 5, 0, input);

            throw nvae;
        }

        switch (alt5) {
            case 1 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:10: T22
                {
                mT22(); 

                }
                break;
            case 2 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:14: T23
                {
                mT23(); 

                }
                break;
            case 3 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:18: T24
                {
                mT24(); 

                }
                break;
            case 4 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:22: T25
                {
                mT25(); 

                }
                break;
            case 5 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:26: CREATE
                {
                mCREATE(); 

                }
                break;
            case 6 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:33: ENSURE
                {
                mENSURE(); 

                }
                break;
            case 7 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:40: DELETE
                {
                mDELETE(); 

                }
                break;
            case 8 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:47: DROP
                {
                mDROP(); 

                }
                break;
            case 9 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:52: ALTER
                {
                mALTER(); 

                }
                break;
            case 10 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:58: RENAME
                {
                mRENAME(); 

                }
                break;
            case 11 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:65: IN
                {
                mIN(); 

                }
                break;
            case 12 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:68: TO
                {
                mTO(); 

                }
                break;
            case 13 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:71: UNIQUE
                {
                mUNIQUE(); 

                }
                break;
            case 14 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:78: LOWER
                {
                mLOWER(); 

                }
                break;
            case 15 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:84: CASCADE
                {
                mCASCADE(); 

                }
                break;
            case 16 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:92: UPDATES
                {
                mUPDATES(); 

                }
                break;
            case 17 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:100: INDEX
                {
                mINDEX(); 

                }
                break;
            case 18 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:106: KEY
                {
                mKEY(); 

                }
                break;
            case 19 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:110: ID
                {
                mID(); 

                }
                break;
            case 20 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:113: WS
                {
                mWS(); 

                }
                break;
            case 21 :
                // C:\\Dev\\workspace\\jSuneido\\src\\suneido\\database\\query\\Request.g:1:116: COMMENT
                {
                mCOMMENT(); 

                }
                break;

        }

    }


 

}