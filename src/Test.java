import tokenizer.StringIter;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.Scanner;

public class Test {

    //Tokenizer Test
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        StringIter stringIter = new StringIter(sc);
        String s=sc.nextLine();
        stringIter.linesBuffer.add(s);
        ArrayList<Token> list = new ArrayList<>();
        System.out.println(stringIter.linesBuffer.get(0));
        Tokenizer tokenizer= new Tokenizer(stringIter);
        try{
            Token token = tokenizer.nextToken();
            while(token.getTokenType()!= TokenType.EOF){
                token = tokenizer.nextToken();
                list.add(token);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        System.out.println();
    }
}
