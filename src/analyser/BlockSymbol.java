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
}
