package tokenizer;

import error.TokenizeError;
import error.ErrorCode;
import util.Pos;

public class Tokenizer {
    public static boolean DEBUG = true;
    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        if (!Tokenizer.DEBUG)
            it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isLetter(peek)||peek=='_') {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUInt() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值

            Pos start = new Pos(it.currentPos().row,it.currentPos().col);
            String s = "";
            char c;
            do {
                char k= it.nextChar();
                s += k;
                c = it.peekChar();
            }while (Character.isDigit(c));
            int number = Integer.parseInt(s);
            return new Token(TokenType.UINT_LITERAL,number,start,it.currentPos());

    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串

            Pos start = new Pos(it.currentPos().row,it.currentPos().col);
            String s = "";
            char c;
            do {
                s += it.nextChar();
                c = it.peekChar();
            }while (Character.isLetterOrDigit(c));
            if (s.equals("fn")) return new Token(TokenType.FN_KW,s,start,it.currentPos());
            if (s.equals("let")) return new Token(TokenType.LET_KW,s,start,it.currentPos());
            if (s.equals("const")) return new Token(TokenType.CONST_KW,s,start,it.currentPos());
            if (s.equals("as")) return new Token(TokenType.AS_KW,s,start,it.currentPos());
            if (s.equals("while")) return new Token(TokenType.WHILE_KW,s,start,it.currentPos());
            if (s.equals("if")) return new Token(TokenType.IF_KW,s,start,it.currentPos());
            if (s.equals("else")) return new Token(TokenType.ELSE_KW,s,start,it.currentPos());
            if (s.equals("return")) return new Token(TokenType.RETURN_KW,s,start,it.currentPos());
            if (s.equals("break")) return new Token(TokenType.BREAK_KW,s,start,it.currentPos());
            if (s.equals("continue")) return new Token(TokenType.CONTINUE_KW,s,start,it.currentPos());
            return new Token(TokenType.IDENT,s,start, it.currentPos());

    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
            case '-':
                if (it.peekChar()=='>')
                    return new Token(TokenType.ARROW,"->", it.previousPos(), it.currentPos());
                return new Token(TokenType.MINUS,'-', it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.MUL,'*', it.previousPos(), it.currentPos());
            case '/':
                if (it.peekChar()=='/')
                    return new Token(TokenType.COMMENT,"//", it.previousPos(), it.currentPos());//TODO:注释处理
                return new Token(TokenType.DIV,'/', it.previousPos(), it.currentPos());
            case '=':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.EQ,"==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN,'=', it.previousPos(), it.currentPos());
            case '!':
                if (it.nextChar()!='=')
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.NEQ,"!=", it.previousPos(), it.currentPos());
            case '<':
                if (it.peekChar()=='=')
                    return new Token(TokenType.LE,"<=", it.previousPos(), it.currentPos());
                return new Token(TokenType.LT,'<', it.previousPos(), it.currentPos());
            case '>':
                if (it.peekChar()=='=')
                    return new Token(TokenType.GE,">=", it.previousPos(), it.currentPos());
                return new Token(TokenType.GT,'>', it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN,'(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN,')', it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE,'{', it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE,'}', it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA,',', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON,';', it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON,':', it.previousPos(), it.currentPos());
            case '"'://TODO:STRING
                return new Token(TokenType.STRING_LITERAL,"", it.previousPos(), it.currentPos());
            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
