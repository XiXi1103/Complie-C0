package analyser;

import error.*;
import instruction.Instruction;
import instruction.Operation;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;
import util.Pos;

import javax.xml.validation.TypeInfoProvider;
import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;
    static ArrayList<BlockSymbol> symbolTable = new ArrayList<>();
    int top = -1;                                                       //symbolTable栈顶
    static HashMap<String, FuncInfo> funList = new HashMap<>();                 //name -> id,para_cnt
    int funID = 0;
    int localParaCnt;
    static BlockSymbol globalSymbol = new BlockSymbol();
    String curFunc;//当前运行的函数名


    /** 当前偷看的 token */
    Token peekedToken = null;


//    /** 符号表 */
//    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    private boolean isFirst_vt_stmt() throws CompileError{
        return check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.UINT_LITERAL)
                ||check(TokenType.STRING_LITERAL)||check(TokenType.DOUBLE_LITERAL)
                ||check(TokenType.L_PAREN)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)
                ||check(TokenType.IF_KW)||check(TokenType.WHILE_KW)||check(TokenType.RETURN_KW)
                ||check(TokenType.SEMICOLON)||check(TokenType.L_BRACE);
    }

    /**
     * 寻找标识符，并使用loca或globa或arga命令将地址压入栈顶
     * @param token
     */
    private Type findIdent(Token token) throws CompileError{
        String name = token.getValueString();
        if(globalSymbol.getIdent(name)!=-1){

            instructions.add(new Instruction(Operation.globa,globalSymbol.getIdent(name)));
            return globalSymbol.getType(name);
        }
        for(int i=0;i<symbolTable.size();i++){
            if (symbolTable.get(i).getIdent(name)!=-1){
                if (i==0)
                    instructions.add(new Instruction(Operation.arga,symbolTable.get(i).getIdent(name)));
                else
                    instructions.add(new Instruction(Operation.loca,symbolTable.get(i).getIdent(name)));
                return symbolTable.get(i).getType(name);
            }
        }
        throw new AnalyzeError(ErrorCode.NotDeclared,token.getStartPos());
    }



    private Type analyseTy() throws CompileError{
        Token token = expect(TokenType.IDENT);
        if (token.getValue().equals("void")){
            return Type.VOID;
        }
        else if(token.getValue().equals("int")){
            return Type.INT;
        }
        else if (token.getValue().equals("double")){
            return Type.DOUBLE;
        }
        else throw new Error("expect int, void or double");
    }

    private void analyseStmt() throws CompileError{
//        stmt ->
//               expr_stmt
//             | decl_stmt
//             | if_stmt
//             | while_stmt
//             | return_stmt
//             | block_stmt
//             | empty_stmt
        if (check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.UINT_LITERAL)
                ||check(TokenType.STRING_LITERAL)||check(TokenType.DOUBLE_LITERAL)
                ||check(TokenType.L_PAREN)){
            analyseExpr();
            expect(TokenType.SEMICOLON);
        }
        else if (check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            localParaCnt++;
            analyseDecl_stmt(true);
        }
        else if (check(TokenType.IF_KW)){
            analyseIf_stmt();
        }
        else if (check(TokenType.WHILE_KW)){
            analyseWhile_stmt();
        }
        else if (check(TokenType.RETURN_KW)){
            analyseReturn_stmt();
        }
        else if (check(TokenType.L_BRACE)){
            analyseBlock_stmt();

        }
        else{
            expect(TokenType.SEMICOLON);
        }
    }


    private void analyseDecl_stmt(boolean isLocal) throws CompileError{//是否为局部变量
        //decl_stmt -> let_decl_stmt | const_decl_stmt

        if (check(TokenType.LET_KW)) analyseLet_decl_stmt(isLocal);
        else analyseConst_decl_stmt(isLocal);
    }
    private void analyseLet_decl_stmt(boolean isLocal) throws CompileError{    //初步完成
        //let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
        expect(TokenType.LET_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();

        if (check(TokenType.ASSIGN)){

            BlockSymbol blockSymbol = symbolTable.get(top);
            blockSymbol.addSymbol(name,true,false,type,token.getStartPos());

            if (isLocal)
                instructions.add(new Instruction(Operation.loca, blockSymbol.getOffset(name,token.getStartPos())));//获取该变量的栈偏移
            else
                instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name,token.getStartPos())));

            analyseExpr();
            instructions.add(new Instruction(Operation.store_64));
        }
        else {
            this.symbolTable.get(top).addSymbol((String) token.getValue(),false,false,type,token.getStartPos());
            expect(TokenType.SEMICOLON);
        }


    }

    private void analyseConst_decl_stmt(boolean isLocal) throws CompileError{  //初步完成
        //const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
        expect(TokenType.CONST_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        expect(TokenType.ASSIGN);

        BlockSymbol blockSymbol = this.symbolTable.get(top);
        blockSymbol.addSymbol(name,true,true,type,token.getStartPos());

        if (isLocal)
            instructions.add(new Instruction(Operation.loca, blockSymbol.getOffset(name,token.getStartPos())));//获取该变量的栈偏移
        else
            instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name,token.getStartPos())));

        analyseExpr();
        expect(TokenType.SEMICOLON);

        instructions.add(new Instruction(Operation.store_64));
    }

    private void analyseIf_stmt() throws CompileError{
        //if_stmt -> 'if' expr block_stmt ('else' (block_stmt | if_stmt))?
        expect(TokenType.IF_KW);
        analyseExpr();
        int pointer = instructions.size();
        analyseBlock_stmt();

        instructions.add(pointer, new Instruction(Operation.br_false, instructions.size()-pointer+1));

        if (check(TokenType.ELSE_KW)){
            if (check(TokenType.IF_KW)){
                analyseIf_stmt();
            }
            else analyseBlock_stmt();
        }

    }

    private void analyseWhile_stmt() throws CompileError{
        //while_stmt -> 'while' expr block_stmt
        expect(TokenType.WHILE_KW);
        int pointer1 = instructions.size();
        analyseExpr();
        int pointer2 = instructions.size();

        analyseBlock_stmt();

        instructions.add(new Instruction(Operation.br, pointer1-instructions.size()));//跳回while
        instructions.add(pointer2, new Instruction(Operation.br_false, instructions.size()-pointer2+1));//在while后加跳转，若expr为假，则跳转

    }

    private void analyseReturn_stmt() throws CompileError{
        //return_stmt -> 'return' expr? ';'
        expect(TokenType.RETURN_KW);
        if (funList.get(curFunc).returnType!=Type.VOID)
            instructions.add(new Instruction(Operation.arga,0));
        Type type = analyseExpr();
        if (type!=funList.get(curFunc).returnType)
            throw new Error("函数返回类型不对");

        if (funList.get(curFunc).returnType!=Type.VOID)
            instructions.add(new Instruction(Operation.store_64));
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.ret));
    }

    private void analyseBlock_stmt() throws CompileError{
        expect(TokenType.L_BRACE);
        BlockSymbol blockSymbol = new BlockSymbol();
        symbolTable.add(blockSymbol);
        top++;
        while (isFirst_vt_stmt()){
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
        symbolTable.remove(top);
        top--;
    }


    private void analyseFunc() throws CompileError{

//        function_param -> 'const'? IDENT ':' ty
//        function_param_list -> function_param (',' function_param)*
//                function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt
//               ^~~~      ^~~~~~~~~~~~~~~~~~~~          ^~ ^~~~~~~~~~
//               |              |                        |  |
//               function_name  param_list     return_type  function_body

        localParaCnt = 0;//初始化局部变量个数
        int paraCnt=0;//参数个数
        instructions = new ArrayList<>();//初始化指令集

        expect(TokenType.FN_KW);
        Token token = expect(TokenType.IDENT);
        expect(TokenType.L_PAREN);



        symbolTable = new ArrayList<>();//新建符号表
        top = -1;
        curFunc =token.getValueString();

        if (check(TokenType.CONST_KW)||check(TokenType.IDENT)){
            paraCnt = analyseFuncParaList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Type type = analyseTy();

        funList.put(token.getValueString(),new FuncInfo(funID,paraCnt,type));//添加函数到函数表
        funID++;

        analyseBlock_stmt();

        funList.get(token.getValueString()).localParaCnt=localParaCnt;//函数表中设置函数局部变量个数

        instructions.add(new Instruction(Operation.ret));
        //TODO:打印函数信息


    }

    private int analyseFuncParaList() throws CompileError{
        int cnt=1;
        BlockSymbol.nextOffset=1;//因为0处是返回值
        symbolTable.add(new BlockSymbol());//symbolTable[0]为参数列表，应用arga命令处理！
        top=0;
        analyseFuncPara();
        while (nextIf(TokenType.COMMA)!=null){
            analyseFuncPara();
            cnt++;
        }
        BlockSymbol.nextOffset = 0;//在存局部变量时将offset置为0
        return cnt;
    }

    private void analyseFuncPara() throws CompileError{
        boolean isConstant = nextIf(TokenType.CONST_KW) != null;
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        symbolTable.get(top).addSymbol(name,true,isConstant,type,token.getStartPos());
    }

    private void analyseProgram() throws CompileError {
        //program -> decl_stmt* function*
        while (check(TokenType.LET_KW) || check(TokenType.CONST_KW)) {
            analyseDecl_stmt(false);
        }
        while (check(TokenType.FN_KW)){
            analyseFunc();
        }
        if (funList.get("main")==null)
            throw new Error("expect a main function");

    }
    public boolean isb_op() throws CompileError{
        return check(TokenType.PLUS)||check(TokenType.MINUS)||check(TokenType.MUL)||check(TokenType.DIV)
                ||check(TokenType.EQ)||check(TokenType.ASSIGN)||check(TokenType.NEQ)
                ||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE);

    }




    static HashMap<TokenType,Integer> priorityMap = new HashMap<>();//TODO
    static {
        priorityMap.put(TokenType.ASSIGN,0);
    }
    private Type analyseExpr() throws CompileError{//需要保证调用完成时栈顶就是表达式值
//        expr ->
//                operator_expr
//                        | negate_expr
//                        | assign_expr
//                        | as_expr
//                        | call_expr
//                        | literal_expr
//                        | ident_expr
//                        | group_expr
//消除左递归后:
//( IDENT(null|(call)|=expr) |-expr | (expr) |UINT |DOUBLE |STRING ) {b_op expr| as 'ty'}
        Type returnType;
        if (check(TokenType.IDENT)){
            if (check(TokenType.L_PAREN)){
                returnType = analyseCall_expr();
            }
            else if (check(TokenType.ASSIGN)){
                analyseAssign_expr();
                returnType = Type.VOID;
            }
            else {
                Token token = expect(TokenType.IDENT);
                returnType = findIdent(token);
                instructions.add(new Instruction(Operation.load_64));

            }
        }
        else if (check(TokenType.MINUS)){
            Token token = expect(TokenType.MINUS);
            returnType = analyseExpr();
            if (returnType == Type.DOUBLE)
                instructions.add(new Instruction(Operation.neg_i));
            else if (returnType == Type.INT)
                instructions.add(new Instruction(Operation.neg_i));
            else
                throw new AnalyzeError(ErrorCode.InvalidAssignment,token.getStartPos());
        }
        else if (check(TokenType.L_PAREN)){
            expect(TokenType.L_PAREN);
            returnType = analyseExpr();
            expect(TokenType.R_PAREN);
        }
        else if (check(TokenType.UINT_LITERAL)){
            Token token = expect(TokenType.UINT_LITERAL);
            instructions.add(new Instruction(Operation.push,(int)token.getValue()));
            returnType = Type.INT;
        }
        else if (check(TokenType.DOUBLE_LITERAL)){

            returnType = Type.DOUBLE;
        }
        else if (check(TokenType.STRING_LITERAL)){

            returnType = Type.VOID;
        }
        else throw new Error("illegal expr!");
        while (true){
            if (check(TokenType.AS_KW)){
                expect(TokenType.AS_KW);
                Type type = analyseTy();
                if (returnType!=Type.VOID){
                    returnType = type;
                }
                else throw new Error("非法的类型转换(将void类型转换为其他类型)");
            }
            else if (isb_op()){
                analyseExpr();
            }
            else break;
        }


        return returnType;
    }
    private Type analyseCall_expr() throws CompileError{
        Token token = expect(TokenType.IDENT);
        expect(TokenType.L_PAREN);

        FuncInfo funcInfo = funList.get(token.getValueString());
        if (funcInfo==null) throw new NotDeclaredError(ErrorCode.NotDeclared,token.getStartPos());

        instructions.add(new Instruction(Operation.stackalloc,funcInfo.returnType==Type.VOID?0:1));
        instructions.add(new Instruction(Operation.stackalloc,funcInfo.paraCnt));
        instructions.add(new Instruction(Operation.push,0));//return value
        //TODO:若为赋值语句，没处理报错
        while (!check(TokenType.R_PAREN)){
            analyseExpr();
            if (!check(TokenType.R_PAREN)){
                expect(TokenType.COMMA);
                analyseExpr();
            }
        }
        instructions.add(new Instruction(Operation.call,funcInfo.funID));

        expect(TokenType.R_PAREN);
        return funcInfo.returnType;
    }
    private void analyseAssign_expr() throws CompileError{
        Token token = expect(TokenType.IDENT);
        expect(TokenType.ASSIGN);
        findIdent(token);
        analyseExpr();
        instructions.add(new Instruction(Operation.store_64));
    }













}



















//    private void analyseProgram() throws CompileError {
//        // 程序 -> 'begin' 主过程 'end'
//        // 示例函数，示例如何调用子程序
//        // 'begin'
//        expect(TokenType.Begin);
//
//        analyseMain();
//
//        // 'end'
//        expect(TokenType.End);
//        expect(TokenType.EOF);
//    }
//
//    private void analyseMain() throws CompileError {
//        // 主过程 -> 常量声明 变量声明 语句序列
//        analyseConstantDeclaration();
//        analyseVariableDeclaration();
//        analyseStatementSequence();
//
//    }
//
//    private void analyseConstantDeclaration() throws CompileError {
//        // 示例函数，示例如何解析常量声明
//        // 常量声明 -> 常量声明语句*
//
//        // 如果下一个 token 是 const 就继续
//        while (nextIf(TokenType.Const) != null) {
//            // 常量声明语句 -> 'const' 变量名 '=' 常表达式 ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//
//            // 加入符号表
//            String name = (String) nameToken.getValue();
//            addSymbol(name, true, true, nameToken.getStartPos());
//
//            // 等于号
//            expect(TokenType.Equal);
//
//            // 常表达式
//            var value = analyseConstantExpression();
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 这里把常量值直接放进栈里，位置和符号表记录的一样。
//            // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值，
//            // 我们这里就先不这么干了。
//            instructions.add(new Instruction(Operation.LIT, value));
//        }
//    }
//
//    private void analyseVariableDeclaration() throws CompileError {
//        // 变量声明 -> 变量声明语句*
//
//        // 如果下一个 token 是 var 就继续
//        while (nextIf(TokenType.Var) != null) {
//            // 变量声明语句 -> 'var' 变量名 ('=' 表达式)? ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//            // 变量初始化了吗
//            boolean initialized = false;
//
//            // 下个 token 是等于号吗？如果是的话分析初始化
//            if(check(TokenType.Equal)){
//                expect(TokenType.Equal);
//                initialized=true;
//                analyseExpression();
//            }
//            // 分析初始化的表达式
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 加入符号表，请填写名字和当前位置（报错用）
//            String name = (String) nameToken.getValue();
//            addSymbol(name, initialized, false, nameToken.getStartPos());
//
//            // 如果没有初始化的话在栈里推入一个初始值
//            if (!initialized) {
//                instructions.add(new Instruction(Operation.LIT, 0));
//            }
//        }
//    }
//
//    private void analyseStatementSequence() throws CompileError {
//        // 语句序列 -> 语句*
//        // 语句 -> 赋值语句 | 输出语句 | 空语句
//
//        while (true) {
//            // 如果下一个 token 是……
//            var peeked = peek();
//
//            if (peeked.getTokenType() == TokenType.Ident) {
//                // 调用相应的分析函数
//                // 如果遇到其他非终结符的 FIRST 集呢？
//                analyseAssignmentStatement();
//            }
//            else if (peeked.getTokenType()== TokenType.Print){
//                analyseOutputStatement();
//            }
//            else if (peeked.getTokenType() == TokenType.Semicolon){
//                next();
//            }
//            else {
//                // 都不是，摸了
//                break;
//            }
//        }
////        throw new Error("Not implemented");
//    }
//
//    private int analyseConstantExpression() throws CompileError {
//        // 常表达式 -> 符号? 无符号整数
//        boolean negative = false;
//        if (nextIf(TokenType.Plus) != null) {
//            negative = false;
//        } else if (nextIf(TokenType.Minus) != null) {
//            negative = true;
//        }
//
//        var token = expect(TokenType.Uint);
//
//        int value = (int) token.getValue();
//        if (negative) {
//            value = -value;
//        }
//
//        return value;
//    }
//
//    private void analyseExpression() throws CompileError {
//        // 表达式 -> 项 (加法运算符 项)*
//        // 项
//        analyseItem();
//
//        while (true) {
//            // 预读可能是运算符的 token
//            var op = peek();
//            if (op.getTokenType() != TokenType.Plus && op.getTokenType() != TokenType.Minus) {
//                break;
//            }
//
//            // 运算符
//            next();
//
//            // 项
//            analyseItem();
//
//            // 生成代码
//            if (op.getTokenType() == TokenType.Plus) {
//                instructions.add(new Instruction(Operation.ADD));
//            } else if (op.getTokenType() == TokenType.Minus) {
//                instructions.add(new Instruction(Operation.SUB));
//            }
//        }
//    }
//
//    private void analyseAssignmentStatement() throws CompileError {
//        // 赋值语句 -> 标识符 '=' 表达式 ';'
//
//        // 分析这个语句
//        Token nameToken = expect(TokenType.Ident);
//        expect(TokenType.Equal);
//        analyseExpression();
//        expect(TokenType.Semicolon);
//        // 标识符是什么？
//        String name = (String) nameToken.getValue();
//        var symbol = symbolTable.get(name);
//        if (symbol == null) {
//            // 没有这个标识符
//            throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ nameToken.getStartPos());
//        } else if (symbol.isConstant) {
//            // 标识符是常量
//            throw new AnalyzeError(ErrorCode.AssignToConstant, /* 当前位置 */ nameToken.getStartPos());
//        }
//        // 设置符号已初始化
//        initializeSymbol(name, nameToken.getStartPos());
//
//        // 把结果保存
//        var offset = getOffset(name, nameToken.getStartPos());
//        instructions.add(new Instruction(Operation.STO, offset));
//    }
//
//    private void analyseOutputStatement() throws CompileError {
//        // 输出语句 -> 'print' '(' 表达式 ')' ';'
//
//        expect(TokenType.Print);
//        expect(TokenType.LParen);
//
//        analyseExpression();
//
//        expect(TokenType.RParen);
//        expect(TokenType.Semicolon);
//
//        instructions.add(new Instruction(Operation.WRT));
//    }
//
//    private void analyseItem() throws CompileError {
//        // 项 -> 因子 (乘法运算符 因子)*
//
//        // 因子
//        analyseFactor();
//        while (true) {
//            // 预读可能是运算符的 token
//            Token op = peek();
//            if (op.getTokenType() != TokenType.Mult && op.getTokenType() != TokenType.Div) {
//                break;
//            }
//
//            // 运算符
//            next();
//
//            // 因子
//            analyseFactor();
//
//            // 生成代码
//            if (op.getTokenType() == TokenType.Mult) {
//                instructions.add(new Instruction(Operation.MUL));
//            } else if (op.getTokenType() == TokenType.Div) {
//                instructions.add(new Instruction(Operation.DIV));
//            }
//        }
//    }
//
//    private void analyseFactor() throws CompileError {
//        // 因子 -> 符号? (标识符 | 无符号整数 | '(' 表达式 ')')
//
//        boolean negate;
//        if (nextIf(TokenType.Minus) != null) {
//            negate = true;
//            // 计算结果需要被 0 减
//            instructions.add(new Instruction(Operation.LIT, 0));
//        } else {
//            nextIf(TokenType.Plus);
//            negate = false;
//        }
//
//        if (check(TokenType.Ident)) {
//            // 是标识符
//
//            // 加载标识符的值
//            Token nameToken = expect(TokenType.Ident);
//            String name = (String) nameToken.getValue();
//            var symbol = symbolTable.get(name);
//            if (symbol == null) {
//                // 没有这个标识符
//                throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
//            } else if (!symbol.isInitialized) {
//                // 标识符没初始化
//                throw new AnalyzeError(ErrorCode.NotInitialized, nameToken.getStartPos());
//            }
//            var offset = getOffset(name, nameToken.getStartPos());
//            instructions.add(new Instruction(Operation.LOD, offset));
//        } else if (check(TokenType.Uint)) {
//            // 是整数
//            // 加载整数值
//            int value = (int)expect(TokenType.Uint).getValue();
//            instructions.add(new Instruction(Operation.LIT, value));
//        } else if (check(TokenType.LParen)) {
//            // 是表达式
//            // 调用相应的处理函数
//            expect(TokenType.LParen);
//            analyseExpression();
//            expect(TokenType.RParen);
//
//        } else {
//            // 都不是，摸了
//            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
//        }
//
//        if (negate) {
//            instructions.add(new Instruction(Operation.SUB));
//        }
//
//    }
//}
