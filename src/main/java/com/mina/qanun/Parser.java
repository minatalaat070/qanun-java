/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 *
 * @author mina
 */
public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current;
    private int loopDepth = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = ternaryCondition();
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr ternaryCondition() {
        Expr expr = or();
        if (match(TokenType.QUESTION_MARK)) {
            Expr trueCondition = ternaryCondition();
            consume(TokenType.COLON, "Expect ':' in ternary operator.");
            Expr falseCondition = ternaryCondition();
            return new Expr.ConditionalTernary(expr, trueCondition, falseCondition);
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.FUN)) {
                return function("function");
            }
            if (match(TokenType.VAR)) {
                return varDeclaration();
            }
            if (match(TokenType.VAL)) {
                return valDeclaration();
            }
            return statement();
        } catch (ParseError parseError) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(TokenType.FOR)) {
            return forStatement();
        }
        if (match(TokenType.IF)) {
            return ifStatement();
        }
        if (match(TokenType.RETURN)) {
            return returnStatement();
        }
        if (match(TokenType.WHILE)) {
            return whileStatement();
        }
        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        if (match(TokenType.BREAK)) {
            return breakStatement();
        }
        if (match(TokenType.CONTINUE)) {
            return continueStatement();
        }
        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after for.");
        Stmt init;
        if (match(TokenType.SEMICOLON)) {
            init = null;
        } else if (match(TokenType.VAR)) {
            init = varDeclaration();
        } else {
            init = expressionStatement();
        }
        Expr condition = null;
        if (!match(TokenType.SEMICOLON)) {
            condition = expression();
            consume(TokenType.SEMICOLON, "Expect ';' after condition.");
        } else {
            condition = new Expr.Literal(true);
        }
        Expr increment = null;
        if (!match(TokenType.RIGHT_PAREN)) {
            increment = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after for expression.");
        }
        loopDepth++;
        Stmt body = statement();
        loopDepth--;
        // init and increment maybe null so check in interpreter
        // condition can't be null if null means loop for ever by setting condition to true
        return new Stmt.For(init, condition, increment, body);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' or new line after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' or a new line after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt valDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect constant name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' or a new line after constant declaration.");
        return new Stmt.Val(name, initializer);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        loopDepth++;
        Stmt body = statement();
        loopDepth--;
        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' or a new line after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt breakStatement() {
        if (loopDepth == 0) {
            throw error(previous(), "'break' must be used inside a loop");
        }
        consume(TokenType.SEMICOLON, "Expect ';' or a new line after break.");
        return new Stmt.Break(previous());
    }

    private Stmt continueStatement() {
        if (loopDepth == 0) {
            throw error(previous(), "'continue' must be used inside a loop");
        }
        consume(TokenType.SEMICOLON, "Expect ';' or a new line after continue.");
        return new Stmt.Continue(previous());
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' at the end of a block.");
        return statements;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR, TokenType.PERCENTAGE, TokenType.STAR_STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                // check to see if the list size is larger than 255 (0 to 254)
                // can be unlimited but for the second interpreter bytecode's sake we keep it limited
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        }
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().getLiteral());
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Qanun.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().getType() == TokenType.SEMICOLON) {
                return;
            }

            switch (peek().getType()) {
                case CLASS:
                case FUN:
                case VAR:
                case VAL:
                case FOR:
                case IF:
                case WHILE:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
