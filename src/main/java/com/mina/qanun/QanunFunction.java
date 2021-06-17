package com.mina.qanun;

import java.util.List;

/**
 *
 * @author mina
 */
public class QanunFunction implements QanunCallable {

	private final String name;
	private final Expr.AnonymousFun declaration;
	private final Environment closure;
	private final boolean isInitializer;

	public QanunFunction(String name, Expr.AnonymousFun declaration, Environment closure, boolean isInitializer) {
		this.name = name;
		this.isInitializer = isInitializer;
		this.declaration = declaration;
		this.closure = closure;
	}

	QanunFunction bind(QanunInstance instance) {
		Environment environment = new Environment(this.closure);
		environment.define(new Token(TokenType.THIS, "this", null, -1), instance);
		return new QanunFunction(this.name, this.declaration, environment, this.isInitializer);
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment environment = new Environment(this.closure);
		for (int i = 0; i < this.declaration.params.size(); i++) {
			environment.define(this.declaration.params.get(i), arguments.get(i));
		}
		try {
			interpreter.executeBlock(this.declaration.body, environment);
		} catch (Return returnValue) {
			if (this.isInitializer) {
				return closure.getAt(0, new Token(TokenType.THIS, "this", null, -1));
			}
			return returnValue.getValue();
		}
		if (this.isInitializer) {
			return closure.getAt(0, new Token(TokenType.THIS, "this", null, -1));
		}
		return null;
	}

	@Override
	public String toString() {
		if (this.name == null) {
			return "<function 'lambda' >";
		}
		return "<function '" + this.name + "'>";
	}

}
