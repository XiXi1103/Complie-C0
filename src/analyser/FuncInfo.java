package analyser;

public class FuncInfo {
    int funID;
    int paraCnt;
    int localParaCnt;
    Type returnType;

    public FuncInfo(int funID, int paraCnt,Type returnType){
        this.returnType = returnType;
        this.funID = funID;
        this.paraCnt = paraCnt;
    }
}
