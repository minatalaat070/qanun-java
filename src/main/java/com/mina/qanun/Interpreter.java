/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author mina
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;

    public Interpreter() {
        defineGlobals();
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Qanun.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.getType()) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                checkDivisionByZero(expr.operator, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PERCENTAGE:
                checkNumberOperands(expr.operator, left, right);
                return (double) left % (double) right;
            case STAR_STAR:
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((double) left, (double) right);

        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        if (!(callee instanceof QanunCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }
        QanunCallable function = (QanunCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected "
                    + function.arity() + " arguments but got "
                    + arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.getType() == TokenType.OR) {
            if (isTruthy(left)) {
                return left;
            }
        } else {
            if (!isTruthy(left)) {
                return left;
            }
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.getType()) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case PLUS_PLUS: {
                if (!(expr.right instanceof Expr.Variable)) {
                    throw new RuntimeError(expr.operator,
                            "Operand of an increment operator must be a variable.");
                }

                checkNumberOperand(expr.operator, right);
                double value = (double) right;
                Expr.Variable variable = (Expr.Variable) expr.right;
                environment.assign(variable.name, value + 1);

                if (expr.isPostFix) {
                    return value;
                } else {
                    return value + 1;
                }
            }
            case MINUS_MINUS: {
                if (!(expr.right instanceof Expr.Variable)) {
                    throw new RuntimeError(expr.operator,
                            "Operand of a decrement operator must be a variable.");
                }

                checkNumberOperand(expr.operator, right);
                double value = (double) right;
                Expr.Variable variable = (Expr.Variable) expr.right;
                environment.assign(variable.name, value - 1);

                if (expr.isPostFix) {
                    return value;
                } else {
                    return value - 1;
                }
            }
        }

        return null;
    }

    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    void execute(Stmt statement) {
        statement.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } finally {
            // returning to the outer environement again after exiting inner block scope
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // executing the block and creating enclosing enviroinmemnt for it
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        if (Qanun.isInRepl) {
            if (stmt.expression instanceof Expr.Call) {
                if (stringify(value).equals("nil")) {
                    return null;
                } else {
                    System.out.println(stringify(value));
                }
            } else {
                System.out.println(stringify(value));
            }
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        QanunFunction function = new QanunFunction(stmt, this.environment);
        environment.define(stmt.name, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name, value);
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        if (stmt.init != null) {
            execute(stmt.init);
        }
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
                if (stmt.increment != null) {
                    evaluate(stmt.increment);
                }
            } catch (BreakJump breakJump) {
                break;
            } catch (ContinueJump continueJump) {
                // automatically increament
                if (stmt.increment != null) {
                    evaluate(stmt.increment);
                }
            }

        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakJump breakJump) {
                break;
            } catch (ContinueJump continueJump) {
                //do nothing just skip the rest of the loop iteration
            }
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        switch (expr.equalSign.getType()) {
            case PLUS_EQUAL: {
                Object currentValue = environment.get(expr.name);
                checkNumberOperands(expr.equalSign, currentValue, value);
                value = (double) currentValue + (double) value;
                break;
            }
            case MINUS_EQUAL: {
                Object currentValue = environment.get(expr.name);
                checkNumberOperands(expr.equalSign, currentValue, value);
                value = (double) currentValue - (double) value;
                break;
            }
            case STAR_EQUAL: {
                Object currentValue = environment.get(expr.name);
                checkNumberOperands(expr.equalSign, currentValue, value);
                value = (double) currentValue * (double) value;
                break;
            }
            case SLASH_EQUAL: {
                Object currentValue = environment.get(expr.name);
                checkNumberOperands(expr.equalSign, currentValue, value);
                checkDivisionByZero(expr.equalSign, value);
                value = (double) currentValue / (double) value;
                break;
            }
            case STAR_STAR_EQUAL: {
                Object currentValue = environment.get(expr.name);
                checkNumberOperands(expr.equalSign, currentValue, value);
                value = Math.pow((double) currentValue, (double) value);
                break;
            }
            case PERCENTAGE_EQUAL: {
                Object currentValue = environment.get(expr.name);
                checkNumberOperands(expr.equalSign, currentValue, value);
                value = (double) currentValue % (double) value;
                break;
            }
        }
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitConditionalTernaryExpr(Expr.ConditionalTernary expr) {
        Object value;
        if (isTruthy(evaluate(expr.condition))) {
            value = evaluate(expr.trueCondition);
        } else {
            value = evaluate(expr.falseCondition);
        }
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    private boolean isTruthy(Object right) {
        if (right == null) {
            return false;
        }
        if (right instanceof Boolean) {
            return (Boolean) right;
        }
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null) {
            return false;
        }
        return left.equals(right);

    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Opernad must be a number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be a number");
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private void checkDivisionByZero(Token operator, Object right) {
        if ((double) right == 0.0) {
            throw new RuntimeError(operator, "/ by zero is illegal");
        }
    }

    @Override
    public Void visitValStmt(Stmt.Val stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        } else {
            throw new RuntimeError(stmt.name, "Uninitialized constant");
        }
        environment.defineConstant(stmt.name, value);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakJump();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueJump();
    }

    private void defineGlobals() {
        nativePrint(new Token(TokenType.IDENTIFIER, "print", null, 0));
        nativePrintln(new Token(TokenType.IDENTIFIER, "println", null, 0));
        nativeClock(new Token(TokenType.IDENTIFIER, "clock", null, 0));
        nativeStr(new Token(TokenType.IDENTIFIER, "str", null, 0));
        nativeLen(new Token(TokenType.IDENTIFIER, "len", null, 0));
        nativeNum(new Token(TokenType.IDENTIFIER, "num", null, 0));
        nativeRead(new Token(TokenType.IDENTIFIER, "read", null, 0));
        nativeReadLine(new Token(TokenType.IDENTIFIER, "readln", null, 0));
        nativeReadFile(new Token(TokenType.IDENTIFIER, "readFile", null, 0));
        nativeWriteFile(new Token(TokenType.IDENTIFIER, "writeFile", null, 0));
        nativeAppendFile(new Token(TokenType.IDENTIFIER, "appendFile", null, 0));
        nativeClear(new Token(TokenType.IDENTIFIER, "clear", null, 0));

    }

    private void nativePrint(Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                System.out.print(stringify(args.get(0)));
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'print'>";
            }
        });
    }

    private void nativePrintln(Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                System.out.println(stringify(args.get(0)));
                return null;
            }

            @Override
            public String toString() {
                return "<native function 'println'>";
            }
        });
    }

    private void nativeClock(Token token) {
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

    private void nativeStr(Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return stringify(args.get(0));
            }

            @Override
            public String toString() {
                return "<native function 'str'>";
            }
        });
    }

    private void nativeLen(Token token) {
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
                        return stringify(arg).length();
                    } else if (arg != null && args instanceof List) {
                        return ((List) arg).size();
                    }
                } catch (ClassCastException castException) {
                    throw new RuntimeError(new Token(TokenType.IDENTIFIER, stringify(args.get(0)), stringify(args.get(0)), 0), "Only strings or lists can have length");
                }
                return null;
            }
        });
    }

    private void nativeNum(Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    return Double.parseDouble(stringify(args.get(0)));
                } catch (NumberFormatException numberFormatException) {
                    throw new RuntimeError(new Token(TokenType.IDENTIFIER, stringify(args.get(0)), stringify(args.get(0)), 0), "Only strings with numeric digits can be casted to numbers");
                }
            }
        });
    }

    private void nativeRead(Token token) {
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

    private void nativeReadLine(Token token) {
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

    private void nativeReadFile(Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                String contents;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(stringify(args.get(0))));
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

    private void nativeWriteFile(Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    try ( // File path is 1st argument
                             BufferedWriter bw = new BufferedWriter(new FileWriter(stringify(args.get(0))))) {
                        // Data is 2nd argument
                        bw.write(stringify(args.get(1)));
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

    private void nativeAppendFile(Token token) {
        globals.define(token, new QanunCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    try ( // File path is 1st argument
                             BufferedWriter bw = new BufferedWriter(new FileWriter(stringify(args.get(0)), true))) {
                        // Data is 2nd argument
                        bw.append(stringify(args.get(1)));
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

    private void nativeClear(Token token) {
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
}
