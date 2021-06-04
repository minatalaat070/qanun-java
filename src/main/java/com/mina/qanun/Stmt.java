package com.mina.qanun;

import java.util.List;

abstract class Stmt {

	interface Visitor<R> {

		R visitBlockStmt(Block stmt);

		R visitExpressionStmt(Expression stmt);

		R visitFunctionStmt(Function stmt);

		R visitClassStmt(Class stmt);

		R visitIfStmt(If stmt);

		R visitReturnStmt(Return stmt);

		R visitVarStmt(Var stmt);

		R visitValStmt(Val stmt);

		R visitWhileStmt(While stmt);

		R visitForStmt(For stmt);

		R visitForEachStmt(ForEach stmt);

		R visitBreakStmt(Break stmt);

		R visitContinueStmt(Continue stmt);

		R visitSwitchStmt(Switch stmt);
	}

	static class Block extends Stmt {

		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}

		final List<Stmt> statements;
	}

	static class Expression extends Stmt {

		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

		final Expr expression;
	}

	static class Function extends Stmt {

		Function(Token name, List<Token> params, List<Stmt> body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStmt(this);
		}

		final Token name;
		final List<Token> params;
		final List<Stmt> body;
	}

	static class Class extends Stmt {

		Class(Token name, Expr.Variable superClass, List<Stmt.Function> methods, List<Stmt.Function> staticMethods) {
			this.name = name;
			this.superClass = superClass;
			this.methods = methods;
			this.staticMethods = staticMethods;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}

		final Token name;
		final Expr.Variable superClass;
		final List<Stmt.Function> methods;
		final List<Stmt.Function> staticMethods;
	}

	static class If extends Stmt {

		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}

		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;
	}

	static class Return extends Stmt {

		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}

		final Token keyword;
		final Expr value;
	}

	static class Var extends Stmt {

		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}

		final Token name;
		final Expr initializer;
	}

	static class Val extends Stmt {

		Val(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitValStmt(this);
		}

		final Token name;
		final Expr initializer;
	}

	static class While extends Stmt {

		While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}

		final Expr condition;
		final Stmt body;
	}

	static class For extends Stmt {

		For(Stmt init, Expr condition, Expr increment, Stmt body) {
			this.init = init;
			this.condition = condition;
			this.increment = increment;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitForStmt(this);
		}

		final Stmt init;
		final Expr condition;
		final Expr increment;
		final Stmt body;
	}

	static class ForEach extends Stmt {

		ForEach(Stmt init, Expr iterable, Stmt body) {
			this.init = init;
			this.iterable = iterable;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitForEachStmt(this);
		}

		final Stmt init;
		final Expr iterable;
		final Stmt body;
	}

	static class Break extends Stmt {

		Break(Token name) {
			this.name = name;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakStmt(this);
		}

		final Token name;
	}

	static class Continue extends Stmt {

		Continue(Token name) {
			this.name = name;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitContinueStmt(this);
		}

		final Token name;
	}

	static class Switch extends Stmt {

		Switch(Expr expression, List<Object> values, List<List<Stmt>> actions) {
			this.expression = expression;
			this.values = values;
			this.actions = actions;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSwitchStmt(this);
		}

		final Expr expression;
		final List<Object> values;
		final List<List<Stmt>> actions;
	}

	abstract <R> R accept(Visitor<R> visitor);
}
