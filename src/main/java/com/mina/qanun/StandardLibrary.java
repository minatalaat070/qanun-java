package com.mina.qanun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 *
 * @author mina
 */
public class StandardLibrary {

    static void defineGlobals(Environment globals) {

        nativePrint(globals, new Token(TokenType.IDENTIFIER, "print", null, 0));
        nativePrintln(globals, new Token(TokenType.IDENTIFIER, "println", null, 0));
        nativeClock(globals, new Token(TokenType.IDENTIFIER, "clock", null, 0));
        nativeStr(globals, new Token(TokenType.IDENTIFIER, "str", null, 0));
        nativeLen(globals, new Token(TokenType.IDENTIFIER, "len", null, 0));
        nativeNum(globals, new Token(TokenType.IDENTIFIER, "num", null, 0));
        nativeRead(globals, new Token(TokenType.IDENTIFIER, "read", null, 0));
        nativeReadLine(globals, new Token(TokenType.IDENTIFIER, "readln", null, 0));
        nativeReadFile(globals, new Token(TokenType.IDENTIFIER, "readFile", null, 0));
        nativeWriteFile(globals, new Token(TokenType.IDENTIFIER, "writeFile", null, 0));
        nativeAppendFile(globals, new Token(TokenType.IDENTIFIER, "appendFile", null, 0));
        nativeClear(globals, new Token(TokenType.IDENTIFIER, "clear", null, 0));
        nativeType(globals, new Token(TokenType.IDENTIFIER, "type", null, 0));

    }

    private static void nativePrint(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                System.out.print(interpreter.stringify(args.get(0)));
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'print'>";
            }
        });
    }

    private static void nativePrintln(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                System.out.println(interpreter.stringify(args.get(0)));
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'println'>";
            }
        });
    }

    private static void nativeClock(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native function 'clock'>";
            }
        });
    }

    private static void nativeStr(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return interpreter.stringify(args.get(0));
            }

            @Override
            public String toString() {
                return "<native function 'str'>";
            }
        });
    }

    private static void nativeLen(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                Object arg = args.get(0);
                try {
                    if (arg instanceof String) {
                        return (double) interpreter.stringify(arg).length();
                    } else if (arg != null && args instanceof List) {
                        return (double) ((List) arg).size();
                    }
                } catch (ClassCastException castException) {
                    throw new RuntimeError(new Token(TokenType.IDENTIFIER, interpreter.stringify(args.get(0)), interpreter.stringify(args.get(0)), 0), "Only strings or lists can have length");
                }
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'len'>";
            }
        });
    }

    private static void nativeNum(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    return Double.parseDouble(interpreter.stringify(args.get(0)));
                } catch (NumberFormatException numberFormatException) {
                    throw new RuntimeError(new Token(TokenType.IDENTIFIER, interpreter.stringify(args.get(0)), interpreter.stringify(args.get(0)), 0), "Only strings with numeric digits can be casted to numbers");
                }
            }

            @Override
            public String toString() {
                return "<native function 'num'>";
            }
        });
    }

    private static void nativeRead(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    return Character.toString((char) reader.read());
                } catch (IOException exception) {
                }
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'read'>";
            }
        });

    }

    private static void nativeReadLine(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    return reader.readLine();
                } catch (IOException exception) {

                }
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'readln'>";
            }
        });
    }

    private static void nativeReadFile(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                String contents;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(interpreter.stringify(args.get(0))));
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

            @Override
            public String toString() {
                return "<native function 'readFile'>";
            }
        });
    }

    private static void nativeWriteFile(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    try ( // File path is 1st argument
                             BufferedWriter bw = new BufferedWriter(new FileWriter(interpreter.stringify(args.get(0))))) {
                        // Data is 2nd argument
                        bw.write(interpreter.stringify(args.get(1)));
                    }
                    return true;
                } catch (IOException exception) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return "<native function 'writefile'>";
            }
        }
        );
    }

    private static void nativeAppendFile(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    try ( // File path is 1st argument
                             BufferedWriter bw = new BufferedWriter(new FileWriter(interpreter.stringify(args.get(0)), true))) {
                        // Data is 2nd argument
                        bw.append(interpreter.stringify(args.get(1)));
                    }
                    return true;
                } catch (IOException exception) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return "<native function 'appendFile'>";
            }
        });
    }

    private static void nativeClear(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'clear'>";
            }
        });
    }

    private static void nativeType(Environment globals, Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                if (arguments.get(0) instanceof String) {
                    return "string";
                } else if (arguments.get(0) instanceof Double) {
                    return "double";
                } else if (arguments.get(0) instanceof Boolean) {
                    return "boolean";
                } else if (arguments.get(0) instanceof QanunCallable) {
                    return "native function";
                } else if (arguments.get(0) instanceof QanunFunction) {
                    return "function";
                } else if (arguments.get(0) instanceof List) {
                    return "list";
                } else if (arguments.get(0) == null) {
                    return "nil";
                } else {
                    return "unknown";
                }
            }

            @Override
            public String toString() {
                return "<native function 'type'>";
            }
        });
    }

}
