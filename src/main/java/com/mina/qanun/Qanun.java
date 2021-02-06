package com.mina.qanun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 *
 * @author mina
 */
public class Qanun {

    public enum Error {
        EX_USAGE(64), EX_DATAERR(65), EX_SOFTWARE(70);

        private final int code;

        private Error(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    static boolean isInRepl;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage qanun [script]");
            System.exit(Error.EX_USAGE.getCode());
        } else if (args.length == 1) {
            isInRepl = false;
            runFile(args[0]);
        } else {
            isInRepl = true;
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(Error.EX_DATAERR.getCode());
        }
        if (hadRuntimeError) {
            System.exit(Error.EX_SOFTWARE.getCode());
        }
    }

    private static void runPrompt() throws IOException {
        try ( InputStreamReader input = new InputStreamReader(System.in);  BufferedReader reader = new BufferedReader(input)) {
            for (;;) {
                System.out.print("Qanun>> ");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (!line.isEmpty()) {
                    run(line + "\n");
                }
                hadError = false;
            }
        }
    }

    private static void run(String source) {

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        // Stop if there was a syntax error.
        if (hadError) {
            return;
        }
        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.getType() == TokenType.EOF) {
            report(token.getLine(), " at end", message);
        } else {
            report(token.getLine(), " at '" + token.getLexeme() + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage()
                + "\n[line " + error.token.getLine() + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}
