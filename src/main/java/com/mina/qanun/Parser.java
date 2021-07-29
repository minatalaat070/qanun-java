package com.mina.qanun;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mina
 */
public class Parser {

	private static class ParseError extends RuntimeException {
	}

	private final List<Token> tokens;
	private int current;
	private boolean maybeNamedArrowFun;

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

	private Stmt declaration() {
		try {
			if (match(TokenType.CLASS)) {
				return classDeclaration();
			}
			if (check(TokenType.FUN) && checkNext(TokenType.IDENTIFIER)) {
				consume(TokenType.FUN, null);
				maybeNamedArrowFun = true;
				Stmt.Function fun = function("function");
				maybeNamedArrowFun = false;
				return fun;
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

	// class Cat "[" : Animal "]" { }
	// "[" optional "]"
	private Stmt classDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
		Expr.Variable superClass = null;
		if (match(TokenType.COLON)) {
			consume(TokenType.IDENTIFIER, "Expect superclass name.");
			superClass = new Expr.Variable(previous());
		}
		consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");
		List<Stmt.Function> methods = new ArrayList<>();
		List<Stmt.Function> staticMethods = new ArrayList<>();
		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			if (match(TokenType.STATIC)) {
				consume(TokenType.FUN, "Expect 'fun' keword after 'static'.");
				staticMethods.add(function("method"));
			} else if (match(TokenType.FUN)) {
				methods.add(function("method"));
			} else {
				throw error(peek(), "Expect 'fun' or 'static fun' before '" + peek().getLexeme() + "' method declaration");
			}
		}
		consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
		return new Stmt.Class(name, superClass, methods, staticMethods);
	}

	private Stmt.Function function(String kind) {
		Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
		return new Stmt.Function(name, functionBody(kind));
	}

	private Expr.AnonymousFun functionBody(String kind) {
		List<Token> paramaters = null;
		if (!kind.equals("method") || check(TokenType.LEFT_PAREN)) {
			consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + "name.");
			paramaters = new ArrayList<>();
			if (!check(TokenType.RIGHT_PAREN)) {
				do {
					if (paramaters.size() >= 255) {
						error(peek(), "Cannot have more that 255 paramaters.");
					}

					paramaters.add(consume(TokenType.IDENTIFIER, "Expect paramater name."));
				} while (match(TokenType.COMMA));
			}
			consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
		}
		if (check(TokenType.ARROW)) {
			consume(TokenType.ARROW, "Expect '->' after '(' ");
			return arrowFun(paramaters);
		}
		consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Expr.AnonymousFun(paramaters, body);
	}

	private Expr.AnonymousFun arrowFun(List<Token> parameters) {
		// fun x () -> expr;
		Expr expr = expression();
		if (maybeNamedArrowFun) {
			consume(TokenType.SEMICOLON, "Expect ';' after expression");
		}
		return new Expr.AnonymousFun(parameters, List.of(new Stmt.Return(null, expr)));
	}

	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
		Expr initializer = null;
		if (match(TokenType.EQUAL)) {
			initializer = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt valDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect constant name.");
		Expr initializer = null;
		if (match(TokenType.EQUAL)) {
			initializer = expression();
		} else {
			Qanun.error(name, "constant varaibles must be initialized");
		}
		consume(TokenType.SEMICOLON, "Expect ';' after constant declaration.");
		return new Stmt.Val(name, initializer);
	}

	private Stmt statement() {
		if (match(TokenType.IMPORT)) {
			return importStatement();
		}
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
		if (match(TokenType.SWITCH)) {
			return switchStatement();
		}
		if (match(TokenType.LEFT_BRACE)) {
			return new Stmt.Block(block());
		}
		if (match(TokenType.BREAK)) {
			return breakStatement(previous());
		}
		if (match(TokenType.CONTINUE)) {
			return continueStatement(previous());
		}
		return expressionStatement();
	}

	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Expression(expr);
	}

	private Stmt importStatement() {
		Token keyword = previous();
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after module name");
		return new Stmt.Import(keyword, expr);
	}

	private Stmt forStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after for.");
		Stmt init;
		if (match(TokenType.SEMICOLON)) {
			init = null;
		} else if (match(TokenType.VAR)) {
			// look ahead to see if it is in for each loop or regular for
			if (checkNext(TokenType.COLON)) {
				init = forEachVarDeclaration();
				return foreachStatement(init);
			}
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
		Stmt body = statement();
		// init and increment maybe null so check in interpreter
		// condition can't be null if null means loop for ever by setting condition to true
		// wrapping it in Block to prevent scope leaks
		return new Stmt.Block(List.of(new Stmt.For(init, condition, increment, body)));
	}

	private Stmt foreachStatement(Stmt init) {
		Expr iterable = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after for expression.");
		Stmt body = statement();
		// wrapping it in Block to prevent scope leaks
		return new Stmt.Block(List.of(new Stmt.ForEach(init, iterable, body)));
	}

	private Stmt forEachVarDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
		Expr initializer = null;
		consume(TokenType.COLON, "Expect ':' after foreach variable declaration.");
		return new Stmt.Var(name, initializer);
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

	private Stmt whileStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
		Stmt body = statement();
		return new Stmt.While(condition, body);
	}

	private Stmt switchStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'switch'.");
		Expr expr = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
		consume(TokenType.LEFT_BRACE, "Expect '{' at start of switch statement.");
		List<List<Stmt>> actions = new ArrayList();
		List<Object> values = new ArrayList();
		while (!match(TokenType.RIGHT_BRACE)) {
			if (isAtEnd()) {
				Qanun.error(peek(), "Unexpected End of File.");
			} else if (match(TokenType.CASE)) {
				if (!match(TokenType.STRING, TokenType.NUMBER, TokenType.TRUE, TokenType.FALSE, TokenType.NIL)) {
					error(peek(), "Case expressions must be constants.");
				}
				Object value = previous().getLiteral();
				if (values.indexOf(value) != -1) {
					error(peek(), "Case expressions must be unique.");
				}
				consume(TokenType.COLON, "Expect ':' after case.");
				List<Stmt> action = new ArrayList();
				while (!check(TokenType.CASE) && !check(TokenType.DEFAULT) && !check(TokenType.RIGHT_BRACE)) {
					action.add(statement());
				}
				values.add(value);

				actions.add(action);
			} else if (match(TokenType.DEFAULT)) {
				if (values.indexOf("default") != -1) {
					error(peek(), "Duplicate default stmt.");
				}
				consume(TokenType.COLON, "Expect ':' after case.");
				List<Stmt> action = new ArrayList();
				while (!check(TokenType.CASE) && !check(TokenType.DEFAULT) && !check(TokenType.RIGHT_BRACE)) {
					action.add(statement());
				}
				values.add("default");
				actions.add(action);
			} else {
				error(peek(), "Unexpected token in middle of switch block.");
				break;
			}
		}
		return new Stmt.Switch(expr, values, actions);
	}

	private Stmt breakStatement(Token breakToken) {
		consume(TokenType.SEMICOLON, "Expect ';' after break.");
		return new Stmt.Break(breakToken);
	}

	private Stmt continueStatement(Token continueToken) {
		consume(TokenType.SEMICOLON, "Expect ';' after continue.");
		return new Stmt.Continue(continueToken);
	}

	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();
		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		consume(TokenType.RIGHT_BRACE, "Expect '}' at the end of a block.");
		return statements;
	}

	private Expr expression() {
		return assignment();
	}

	private Expr assignment() {
		Expr expr = ternaryCondition();
		if (match(TokenType.EQUAL, TokenType.PLUS_EQUAL,
				TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL,
				TokenType.SLASH_EQUAL, TokenType.STAR_STAR_EQUAL, TokenType.PERCENTAGE_EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value, equals);
			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get) expr;
				return new Expr.Set(get.object, get.name, value);
			} else if (expr instanceof Expr.ListAccessor) {
				Token name = ((Expr.ListAccessor) expr).name;
				return new Expr.ListMutator(expr, name, value);
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
			return new Expr.Unary(operator, right, false);
		}
		return exponenet();
	}

	private Expr exponenet() {
		Expr expr = prefix();
		while (match(TokenType.STAR_STAR)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr prefix() {
		if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
			Token operator = previous();
			Expr right = primary();
			return new Expr.Unary(operator, right, false);
		}
		return postfix();
	}

	private Expr postfix() {
		if (matchNext(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
			Token operator = peek();
			current--;
			Expr left = primary();
			advance();
			return new Expr.Unary(operator, left, true);
		}
		return call();
	}

	private Expr call() {
		Expr expr = primary();
		Token exprName = previous();
		while (true) {
			if (match(TokenType.LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if (match(TokenType.DOT)) {
				Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
				expr = new Expr.Get(expr, name);
			} else if (match(TokenType.LEFT_SQUARE_BRACKET)) {
				Expr index = expression();
				consume(TokenType.RIGHT_SQUARE_BRACKET,
						"Expect ']' after subscript index.");
				expr = new Expr.ListAccessor(expr, exprName, index);
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
		if (match(TokenType.FUN)) {
			return functionBody("function");
		}
		if (match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().getLiteral());
		}
		if (match(TokenType.THIS)) {
			return new Expr.This(previous());
		}
		if (match(TokenType.SUPER)) {
			Token keyword = previous();
			consume(TokenType.DOT, "Expect '.' after 'super'.");
			Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name.");
			return new Expr.Super(keyword, method);
		}
		if (match(TokenType.IDENTIFIER)) {
			return new Expr.Variable(previous());
		}
		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		if (match(TokenType.LEFT_SQUARE_BRACKET)) {
			List<Expr> items = new ArrayList<>();
			if (!check(TokenType.RIGHT_SQUARE_BRACKET)) {
				do {
					items.add(expression());
				} while (match(TokenType.COMMA));
			}
			consume(TokenType.RIGHT_SQUARE_BRACKET, "Expect ']' after list.");
			return new Expr.QanunList(items);
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

	private boolean matchNext(TokenType... types) {
		for (TokenType type : types) {
			if (checkNext(type)) {
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

	private boolean checkNext(TokenType tokenType) {
		if (isAtEnd()) {
			return false;
		}
		if (tokens.get(current + 1).getType() == TokenType.EOF) {
			return false;
		}
		return tokens.get(current + 1).getType() == tokenType;
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
				case BREAK:
				case CONTINUE:
					return;
			}

			advance();
		}
	}
}
