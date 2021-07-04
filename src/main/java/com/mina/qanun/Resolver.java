package com.mina.qanun;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *
 * @author mina
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

	private final Interpreter interpreter;
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();
	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;
	private boolean isInLoop;

	private enum FunctionType {
		NONE,
		FUNCTION,
		INITIALIZER,
		METHOD
	}

	private enum ClassType {
		NONE,
		CLASS,
		SUBCLASS
	}

	public Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public Void visitAssignExpr(Expr.Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Expr.Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitQanunListExpr(Expr.QanunList expr) {
		for (Expr expression : expr.list) {
			resolve(expression);
		}
		return null;
	}

	@Override
	public Void visitCallExpr(Expr.Call expr) {
		resolve(expr.callee);

		for (Expr argument : expr.arguments) {
			resolve(argument);
		}

		return null;
	}

	@Override
	public Void visitGetExpr(Expr.Get expr) {
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitGroupingExpr(Expr.Grouping expr) {
		resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Expr.Literal expr) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Expr.Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitSetExpr(Expr.Set expr) {
		resolve(expr.value);
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitSuperExpr(Expr.Super expr) {
		if (currentClass == ClassType.NONE) {
			Qanun.error(expr.keyword, "Can't use 'super' outside of a class.");
		} else if (currentClass != ClassType.SUBCLASS) {
			Qanun.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitThisExpr(Expr.This expr) {
		if (currentClass == ClassType.NONE) {
			Qanun.error(expr.keyword, "Can't use 'this' outside of a class.");
			return null;
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitListAccessorExpr(Expr.ListAccessor expr) {
		resolve(expr.object);
		resolve(expr.index);
		return null;
	}

	@Override
	public Void visitListMutatorExpr(Expr.ListMutator expr) {
		resolve(expr.object);
		resolve(expr.value);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Expr.Unary expr) {
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitAnonymousFunExpr(Expr.AnonymousFun expr) {
		resolveFunction(expr, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitVariableExpr(Expr.Variable expr) {
		if (!this.scopes.isEmpty() && scopes.peek().get(expr.name.getLexeme()) == Boolean.FALSE) {
			Qanun.error(expr.name, "Can't read local variable in its own initializer.");
		}
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitConditionalTernaryExpr(Expr.ConditionalTernary expr) {
		resolve(expr.condition);
		resolve(expr.trueCondition);
		resolve(expr.falseCondition);
		return null;

	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS;
		declare(stmt.name);
		define(stmt.name);
		if (stmt.superClass != null
				&& stmt.name.getLexeme().equals(stmt.superClass.name.getLexeme())) {
			Qanun.error(stmt.superClass.name, "A class can't inherit from itself.");
		}
		if (stmt.superClass != null) {
			currentClass = ClassType.SUBCLASS;
			resolve(stmt.superClass);
		}
		if (stmt.superClass != null) {
			beginScope();
			scopes.peek().put("super", true);
		}
		beginScope();
		scopes.peek().put("this", true);
		for (Stmt.Function method : stmt.staticMethods) {
			beginScope();
			scopes.peek().put("this", true);
			resolveFunction(method.anonFun, FunctionType.METHOD);
			endScope();
		}
		for (Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.getLexeme().equals("init")) {
				declaration = FunctionType.INITIALIZER;
			}
			resolveFunction(method.anonFun, declaration);
		}
		endScope();
		if (stmt.superClass != null) {
			endScope();
		}
		currentClass = enclosingClass;
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		declare(stmt.name);
		/*
		We define the name eagerly, before resolving the function’s body.
		This lets a function recursively refer to itself inside its own body.
		 */
		define(stmt.name);
		resolveFunction(stmt.anonFun, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch != null) {
			resolve(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Qanun.error(stmt.keyword, "Can't return from top-level code.");
		}
		if (stmt.value != null) {
			if (currentFunction == FunctionType.INITIALIZER) {
				Qanun.error(stmt.keyword, "Can't return a value from an initializer.");
			}
			resolve(stmt.value);
		}

		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		declare(stmt.name);
		if (stmt.initializer != null) {
			resolve(stmt.initializer);
		}
		define(stmt.name);
		return null;
	}

	@Override
	public Void visitValStmt(Stmt.Val stmt) {
		declare(stmt.name);
		resolve(stmt.initializer);
		define(stmt.name);
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		isInLoop = true;
		resolve(stmt.condition);
		resolve(stmt.body);
		isInLoop = false;
		return null;
	}

	@Override
	public Void visitSwitchStmt(Stmt.Switch stmt) {
		isInLoop = true;
		for (List<Stmt> listOfStmts : stmt.actions) {
			for (Stmt item : listOfStmts) {
				resolve(item);
			}
		}
		isInLoop = false;
		return null;
	}

	@Override
	public Void visitForStmt(Stmt.For stmt) {
		isInLoop = true;
		if (stmt.init != null) {
			resolve(stmt.init);
		}
		if (stmt.condition != null) {
			resolve(stmt.condition);
		}
		resolve(stmt.body);
		if (stmt.increment != null) {
			resolve(stmt.increment);
		}
		isInLoop = false;
		return null;
	}

	@Override
	public Void visitForEachStmt(Stmt.ForEach stmt) {
		isInLoop = true;
		resolve(stmt.init);
		resolve(stmt.iterable);
		resolve(stmt.body);
		isInLoop = false;
		return null;
	}

	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		if (!isInLoop) {
			Qanun.error(stmt.name, "break statement is not allowed outside a loop");
		}
		return null;
	}

	@Override
	public Void visitContinueStmt(Stmt.Continue stmt) {
		if (!isInLoop) {
			Qanun.error(stmt.name, "continue statement is not allowed outside a loop");
		}
		return null;
	}

	@Override
	public Void visitImportStmt(Stmt.Import stmt) {
		return null;
	}

	@Override
	public Void visitModuleStmt(Stmt.Module stmt) {
		return null;
	}

	void resolve(List<Stmt> statements) {
		for (Stmt statement : statements) {
			resolve(statement);
		}
	}

	private void resolve(Stmt statement) {
		statement.accept(this);
	}

	private void resolve(Expr expr) {
		expr.accept(this);
	}

	private void beginScope() {
		this.scopes.push(new HashMap<String, Boolean>());
	}

	private void endScope() {
		this.scopes.pop();
	}

	private void declare(Token name) {
		if (this.scopes.isEmpty()) {
			return;
		}

		Map<String, Boolean> scope = this.scopes.peek();
		if (scope.containsKey(name.getLexeme())) {
			Qanun.error(name,
					"Already variable/constant with the same name is in this scope.");
		}
		scope.put(name.getLexeme(), false);
	}

	private void define(Token name) {
		if (this.scopes.isEmpty()) {
			return;
		}
		scopes.peek().put(name.getLexeme(), true);
	}

	private void resolveLocal(Expr expr, Token name) {
		int size = scopes.size() - 1;
		for (int i = size; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.getLexeme())) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}
	}

	private void resolveFunction(Expr.AnonymousFun function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;
		beginScope();
		for (Token param : function.params) {
			declare(param);
			define(param);
		}
		resolve(function.body);
		endScope();
		currentFunction = enclosingFunction;
	}
}
