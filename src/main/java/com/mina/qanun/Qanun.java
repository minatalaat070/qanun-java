package com.mina.qanun;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
		EX_USAGE(64), EX_DATAERR(65), EX_SOFTWARE(70), EX_GENERAL(1);

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
	static String fileName;

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.err.println("Usage: qanun [script.qan | script.qanun]");
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
		String name = Paths.get(path).getFileName().toString();
		Qanun.fileName = name;
		boolean isDotQanFile = name.matches("([a-zA-z1-9]+\\.)+(qanun|qan)$");
		if (!isDotQanFile) {
			System.err.println("Error: Qanun file should end with .qan or .qanun file extension");
			System.exit(Error.EX_GENERAL.getCode());
		}
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
				run(line + "\n");
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
		Resolver resolver = new Resolver(interpreter);
		resolver.resolve(statements);
		if (hadError) {
			return;
		}
		interpreter.interpret(statements);
	}

	static List<Stmt> processModule(String path, Token keyword, Object module) {
		StringBuilder source = new StringBuilder();
		try {
			path += ".qan";
			File file = Paths.get(path).toFile();
			Qanun.fileName = file.getAbsolutePath();
			if (!file.exists()) {
				file = Paths.get(path.replace(".qan", ".qanun")).toFile();
				if (!file.exists()) {
					throw new RuntimeError(keyword, "Module doesn't exisit or file name doesn't end with '.qan' or '.qanun' extenstion.");
				}
			}
			BufferedReader br = new BufferedReader(new FileReader(file));
			String currentLine;
			while ((currentLine = br.readLine()) != null) {
				source.append(currentLine).append("\n");
			}
			Scanner scanner = new Scanner(source.toString());
			List<Token> tokens = scanner.scanTokens();
			Parser parser = new Parser(tokens);
			List<Stmt> statements = parser.parse();
			if (hadError) {
				System.exit(Error.EX_DATAERR.code);
			}
			Resolver resolver = new Resolver(interpreter);
			resolver.resolve(statements);
			if (hadError) {
				System.exit(Error.EX_DATAERR.code);
			}
			return statements;
		} catch (IOException e) {
			throw new RuntimeError(keyword, "Couldn't import module '" + module + "'.");
		}
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
		String infile ="";
		if(isInRepl) infile = "File => '" + Qanun.fileName ;
		System.err.println( infile+"' \n[line " + error.token.getLine() + "] " + error.getMessage());
		hadRuntimeError = true;
	}

	private static void report(int line, String where, String message) {
		String infile ="";
		if(isInRepl) infile = "File => '" + Qanun.fileName ;
		System.err.println( infile+"' \n[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}
}
