package analyser;

import error.AnalyzeError;
import error.ErrorCode;
import util.Pos;

import java.util.HashMap;

public class BlockSymbol {
    private int nextOffset = 0;
    private HashMap<String, SymbolEntry> blockSymbolTable = new HashMap<>();

    public int getNextVariableOffset() {
        return this.nextOffset++;
    }
    public void addSymbol(String name, boolean isInitialized, boolean isConstant,Type type, Pos curPos) throws AnalyzeError {
        if (this.blockSymbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.blockSymbolTable.put(name, new SymbolEntry(type, isConstant, isInitialized, getNextVariableOffset()));
        }
    }
    public void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.blockSymbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }
    public int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.blockSymbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }
    public boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.blockSymbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

//    /**
//     * 获取下一个变量的栈偏移
//     *
//     * @return
//     */
//    private int getNextVariableOffset() {
//        return this.nextOffset++;
//    }

//    /**
//     * 添加一个符号
//     *
//     * @param name          名字
//     * @param isInitialized 是否已赋值
//     * @param isConstant    是否是常量
//     * @param curPos        当前 token 的位置（报错用）
//     * @throws AnalyzeError 如果重复定义了则抛异常
//     */
//    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
//        if (this.symbolTable.get(name) != null) {
//            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
//        } else {
//            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
//        }
//    }

//    /**
//     * 设置符号为已赋值
//     *
//     * @param name   符号名称
//     * @param curPos 当前位置（报错用）
//     * @throws AnalyzeError 如果未定义则抛异常
//     */
//    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbolTable.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            entry.setInitialized(true);
//        }
//    }

//    /**
//     * 获取变量在栈上的偏移
//     *
//     * @param name   符号名
//     * @param curPos 当前位置（报错用）
//     * @return 栈偏移
//     * @throws AnalyzeError
//     */
//    private int getOffset(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbolTable.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            return entry.getStackOffset();
//        }
//    }

//    /**
//     * 获取变量是否是常量
//     *
//     * @param name   符号名
//     * @param curPos 当前位置（报错用）
//     * @return 是否为常量
//     * @throws AnalyzeError
//     */
//    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbolTable.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            return entry.isConstant();
//        }
//    }

}
