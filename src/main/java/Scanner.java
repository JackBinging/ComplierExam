import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 词法分析器
 * @Date 2022/11/14 14:30
 * @Author zhb
 */
public class Scanner {

    // 定义DFA中所有状态表
    private static final int START = 1;             // 开始
    private static final int NUM = 2;               // 数字
    private static final int ID = 3;                // 标识符
    private static final int N2 = 4;                // ++ && 类重叠的运算符
    private static final int N1 = 5;                // = 结尾的运算符
    private static final int N3 = 6;                // >>> <<< >>= <<=
    private static final int NOTE = 7;              // 注释
    private static final int LINENOTE = 8;          // 单行注释
    private static final int MULTLINENOTE1 = 9;     // 多行注释
    private static final int MULTLINENOTE2 = 10;    // 多行注释
    private static final int DONE = 11;             // 结束
    private static final int STRING = 12;           // 字符串

    // 定义种别码对应表
    private enum Code {
        KEYWORD(1, "关键字"),
        SPECIAL(2, "界限符"),
        ARITHMETIC(3, "运算符"),
        ID(4, "标识符"),
        UNKNOWN(5, "常量"),
        SUFFIX(6, "后缀名");

        private final int code;

        Code(int code, String message) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    // 关键字
    private final String[] keyWords = {
            "include", "define", "int", "float", "double", "main", "if",
            "const", "continue", "default", "enum", "extern", "long", "short",
            "else", "for", "while", "do", "goto", "switch", "case", "static",
            "sizeof", "auto", "break", "return", "struct", "typedef", "void",
            "unsigned", "volatile", "union"
    };

    // 特殊字符
    private final String[] special = {
            "{", "}", "[", "]", "(", ")", "#", ",", ".", ";", ":", "?"
    };

    // 运算符
    private final String[] arithmetic = {
            "+", "-", "%", "*", "/", "|", "&", "~", "!", "&&", "||", "|=", "&=", "^=",
            "!=", "=", "==", ">=", "<=", "++", "--", ">", "<", "/=", "*=", "+=", "-=", "%=",
            ">>", ">>=", ">>>", "<<", "<<<", "<<="
    };

    // 源文件输入流
    private final BufferedReader sourceFile;
    // 结果输出流
    private final BufferedWriter resultFile;
    // 保存结果
    private final List<String> ans = new ArrayList<>();
    // 当前行的字符长度
    private int bufSize = 0;
    // 当前行的字符序列
    private char[] lineBuf;
    // 当前行数
    private int lineNum = 0;
    // 当前扫描字符的下标
    private int charPos = 0;
    // 是否到达文件尾
    private boolean isEOF = false;

    /**
     * @Description: 初始化自动机，读取文件
     */
    public Scanner(String in, String out) throws IOException {
        this.sourceFile = new BufferedReader(new FileReader(in));
        this.resultFile = new BufferedWriter(new FileWriter(out));
    }

    /**
     * @Description: 扫描开始，直到读取到文件结束符EOF
     */
    private void scanning() throws IOException {
        while (!isEOF) {
            getToken();
        }
        System.out.println("\nOver!\n");
        write();
    }

    /**
     * @Description: 写入结果进文件
     */
    private void write() throws IOException {
        // 找到最长的键值对
        int maxLen = 0;
        for (String an : ans) {
            if (an.length() > maxLen) {
                maxLen = an.length();
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ans.size(); i++) {
            sb.append(ans.get(i)).append(" ".repeat(maxLen + 2 - ans.get(i).length()));
            // 没四个一行
            if ((i + 1) % 4 == 0) {
                sb.append("\n");
                resultFile.write(sb.toString());
                sb = new StringBuilder();
            }
        }
        sb.append("\n");
        resultFile.write(sb.toString());
        resultFile.close();
    }

    /**
     * @Description: 获取下一个字符
     */
    private char getNextChar() throws IOException {
        char nextChar = '\0';
        // 当前行读完了读下一行
        if (!(charPos < bufSize)) {
            // 当前行不为空
            String eachLine = sourceFile.readLine();
            if (eachLine != null) {
                // 加上换行防止回退时下标越界
                eachLine += "\n";
                lineNum++;
                lineBuf = eachLine.toCharArray();
                bufSize = lineBuf.length;
                charPos = 0;
                nextChar = lineBuf[charPos++];
            } else {
                // 读完了
                isEOF = true;
            }
        } else {
            nextChar = lineBuf[charPos++];
        }
        return nextChar;
    }

    /**
     * @Description: 取消获取下一个字符
     */
    private void unGetNextChar() {
        if (!isEOF) {
            charPos--;
        }
    }

    /**
     * @Description: 获取下一个token
     */
    private void getToken() throws IOException {
        // 当前保存的token
        StringBuilder tokenStr = new StringBuilder();
        // 开始状态
        int currentState = START;
        // 是否保存当前字符进token
        boolean isSave;
        // 读完了结束或者状态位 DONE 结束
        while (currentState != DONE && !isEOF) {
            // 读入一个字符
            char c = getNextChar();
            isSave = true;

            switch (currentState) {
                case START:
                    if (Character.isDigit(c)) {
                        currentState = NUM;
                    } else if (Character.isLetter(c) || c == '.') {                 // 后缀名
                        currentState = ID;
                    } else if (c == ' ' || c == '\t' || c == '\n' || c == '\0') {   // 直接结束
                        isSave = false;
                        currentState = DONE;
                    } else if (c == '!' || c == '=' || c == '*') {                  // = 结尾的运算符
                        currentState = N1;
                    } else if (c == '+' || c == '-' || c == '&' || c == '|' || c == '>' || c == '<' || c == '^') {
                        currentState = N2;                          // 重叠的运算符 & = 结尾的运算符 & 长度为3的运算符
                    } else if (c == '/') {
                        currentState = NOTE;                                        // 注释 & /=
                    } else if (c == '"') {
                        currentState = STRING;                                      // 字符串
                    } else {
                        currentState = DONE;                                        // 单个符号
                    }
                    break;
                case NUM:
                    if (!Character.isDigit(c)) {
                        currentState = DONE;
                        unGetNextChar();
                        isEOF = false;
                        isSave = false;
                    }
                    break;
                case ID:
                    if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_') {
                        currentState = DONE;
                        unGetNextChar();
                        isEOF = false;
                        isSave = false;
                    }
                    break;
                case STRING:
                    if (c == '"') {
                        currentState = DONE;
                    }
                    break;
                case N1:
                    if (c != '=') {
                        unGetNextChar();
                        isEOF = false;
                        isSave = false;
                    }
                    currentState = DONE;
                    break;
                case N2:
                    String s = tokenStr.toString();
                    if (s.equals("+") && c == '+' || s.equals("-") && c == '-' ||
                            s.equals("|") && c == '|' || s.equals("&") && c == '&') {
                        currentState = DONE;
                    } else if (s.equals(">") && c == '>' || s.equals("<") && c == '<') {
                        // 说明有可能是 >> | >>> | >>=
                        currentState = N3;
                    } else {
                        unGetNextChar();
                        isEOF = false;
                        isSave = false;
                        currentState = N1;
                    }
                    break;
                case N3:
                    String ss = tokenStr.toString();
                    if ((!ss.equals(">>") || c != '>' && c != '=') && (!ss.equals("<<") || c != '<' && c != '=')) {
                        // 不接受后面的
                        isEOF = false;
                        isSave = false;
                        unGetNextChar();
                    }
                    // 接收后面的
                    currentState = DONE;
                    break;
                case NOTE:
                    isSave = false;
                    if (c == '/') {
                        tokenStr = new StringBuilder();
                        currentState = LINENOTE;
                    } else if (c == '*') {
                        tokenStr = new StringBuilder();
                        currentState = MULTLINENOTE1;
                    } else {
                        // 说明有可能是 / 或者 /=
                        currentState = N1;
                        unGetNextChar();
                        isEOF = false;
                    }
                    break;
                case LINENOTE:
                    isSave = false;
                    if (c == '\n') {
                        currentState = DONE;
                    }
                    break;
                case MULTLINENOTE1:
                    isSave = false;
                    if (c == '*') {
                        currentState = MULTLINENOTE2;
                    }
                    break;
                case MULTLINENOTE2:
                    isSave = false;
                    if (c == '/') {
                        currentState = DONE;
                    } else {
                        currentState = MULTLINENOTE1;
                    }
                    break;
            }
            if (isSave) {
                tokenStr.append(c);
            }
            // tokenStr不为空才保存
            if (currentState == DONE && !tokenStr.toString().equals("")) {
                ans.add("(" + tokenStr+ " , "+ generateCode(tokenStr.toString()) + ")");
            }
        }
    }

    /**
     * @Description: 生成种别码
     */
    private int generateCode(String token) {
        if (isKeyWord(token)) {
            return Code.KEYWORD.getCode();
        } else if (isSpecial(token)) {
            return Code.SPECIAL.getCode();
        } else if (isArithmetic(token)) {
            return Code.ARITHMETIC.getCode();
        } else if (token.matches("[a-zA-Z_]+")){
            return Code.ID.getCode();
        }else if (token.startsWith(".")){
            return Code.SUFFIX.getCode();
        } else {
            return Code.UNKNOWN.getCode();
        }
    }

    /**
     * @Description: 判断token是不是特殊字符（界限符）
     */
    private boolean isSpecial(String token) {
        for (String s : special) {
            if (token.equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @Description: 判断token是不是运算符
     */
    private boolean isArithmetic(String token) {
        for (String s : arithmetic) {
            if (token.equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @Description: 判断token是不是关键字
     */
    private boolean isKeyWord(String token) {
        for (String s : keyWords) {
            if (token.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner("src/main/resources/in.c", "src/main/resources/out.txt");
        scanner.scanning();
    }

}