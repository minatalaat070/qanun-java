/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mina
 */
class BuiltInModules {

	static final QanunNativeInstance File = new QanunNativeInstance("File", new HashMap<String, QanunCallable>() {
		{
			put("readFile", new QanunCallable() {
				@Override
				public int arity() {
					return 1;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> arguments) {
					String contents;
					try {
						BufferedReader bufferedReader = new BufferedReader(new FileReader(interpreter.stringify(arguments.get(0))));
						String line;
						contents = "";
						while ((line = bufferedReader.readLine()) != null) {
							contents += line + "\n";
						}
					} catch (IOException exception) {
						return null;
					}
					if (contents.length() > 0 && contents.charAt(contents.length() - 1) == '\n') {
						contents = contents.substring(0, contents.length() - 1);
					}
					return contents;
				}
			});

			put("writeFile", new QanunCallable() {
				@Override
				public int arity() {
					return 2;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> arguments) {
					try {
						try ( // File path is 1st argument
								 BufferedWriter bw = new BufferedWriter(new FileWriter(interpreter.stringify(arguments.get(0))))) {
							// Data is 2nd argument
							bw.write(interpreter.stringify(arguments.get(1)));
						}
						return true;
					} catch (IOException exception) {
						return false;
					}
				}
			});

			put("appenFile", new QanunCallable() {
				@Override
				public int arity() {
					return 2;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> arguments) {
					try {
						try ( // File path is 1st argument
								 BufferedWriter bw = new BufferedWriter(new FileWriter(interpreter.stringify(arguments.get(0)), true))) {
							// Data is 2nd argument
							bw.append(interpreter.stringify(arguments.get(1)));
						}
						return true;
					} catch (IOException exception) {
						return false;
					}
				}
			});

		}
	});
	static final QanunNativeInstance Time = new QanunNativeInstance("Time", new HashMap<String, QanunCallable>() {
		{
			put("time", new QanunCallable() {
				@Override
				public int arity() {
					return 0;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> args) {
					Date date = new Date();
					SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
					return formatter.format(date);
				}
			});

			put("date", new QanunCallable() {
				@Override
				public int arity() {
					return 0;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> args) {
					Date date = new Date();
					SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
					return formatter.format(date);
				}
			});

			put("dateAndTime", new QanunCallable() {
				@Override
				public int arity() {
					return 0;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> args) {
					Date date = new Date();
					SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
					return formatter.format(date);
				}
			});
		}
	});
	static final QanunNativeInstance Crypto = new QanunNativeInstance("Crypto", new HashMap<String, QanunCallable>() {
		{
			put("sha", new QanunCallable() {
				@Override
				public int arity() {
					return 1;
				}

				@Override
				public Object call(Interpreter interpreter, List<Object> args) {
					String originalString = (String) args.get(0);
					MessageDigest digest;
					try {
						digest = MessageDigest.getInstance("SHA3-256");
						byte[] hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
						return bytesToHex(hash);
					} catch (NoSuchAlgorithmException e) {
						System.err.println("Oops, couldn't hash your string.");
					}

					return null;
				}
			});
		}
	});

	static void importAll(Environment environment) {
		environment.define(new Token(null, BuiltInModules.File.getName(), null, -1), File);
		environment.define(new Token(null, BuiltInModules.Time.getName(), null, -1), Time);
		environment.define(new Token(null, BuiltInModules.Crypto.getName(), null, -1), Crypto);

	}

	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : hash) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append("0");
			}
			hexString.append(hex);
		}

		return hexString.toString();
	}

}
