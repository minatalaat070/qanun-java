package com.mina.qanun;

import java.util.List;

/**
 *
 * @author mina
 */
public class QanunFunction implements QanunCallable {

	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;

	public QanunFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
		this.isInitializer = isInitializer;
		this.declaration = declaration;
		this.closure = closure;
	}

	QanunFunction bind(QanunInstance instance) {
		Environment environment = new Environment(this.closure);
		environment.define(new Token(TokenType.THIS, "this", null, -1), instance);
		return new QanunFunction(declaration, environment, isInitializer);
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment environment = new Environment(this.closure);
		for (int i = 0; i < declaration.params.size(); i++) {
			environment.define(declaration.params.get(i), arguments.get(i));
		}
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (Return returnValue) {
			if (isInitializer) {
				return closure.getAt(0, new Token(TokenType.THIS, "this", null, -1));
			}
			return returnValue.getValue();
		}
		if (isInitializer) {
			return closure.getAt(0, new Token(TokenType.THIS, "this", null, -1));
		}
		return null;
	}

	@Override
	public String toString() {
		return "<function '" + declaration.name.getLexeme() + "'>";
	}

}
