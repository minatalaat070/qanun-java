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
        EX_USAGE(64), EX_DATAERR(65);

        private final int code;

        private Error(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage qanun [script]");
            System.exit(Error.EX_USAGE.getCode());
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(Error.EX_DATAERR.getCode());
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
                run(line + "\n");
                hadError = false;
            }
        }
    }

    private static void run(String source) {

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        if (hadError) {
            return;
        }
        // For now, just print the tokens.
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}
