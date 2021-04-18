package com.mina.qanun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mina
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	final Environment globals = new Environment();
	private Environment environment = globals;
	private final Map<Expr, Integer> locals = new HashMap<>();

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
				if (left instanceof List && right instanceof List) {
					List leftCasted = (List) left;
					List tmp = new ArrayList();
					for (Object object : leftCasted) {
						tmp.add(object);
					}
					tmp.addAll((List) right);
					return tmp;
				}
				throw new RuntimeError(expr.operator,
						"Operands must be two numbers or two strings or two lists.");
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
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if (object instanceof QanunInstance) {
			return ((QanunInstance) object).get(expr.name);
		}
		throw new RuntimeError(expr.name,
				"Only instances have properties.");
	}

	@Override
	public Object visitListAccessorExpr(Expr.ListAccessor expr) {
		Object listObject = evaluate(expr.object);
		if (listObject instanceof List) {
			List list = (List) listObject;

			Object indexObject = evaluate(expr.index);
			if (!(indexObject instanceof Double)) {
				throw new RuntimeError(expr.name,
						"Only numbers can be used as a list index.");
			}
			int indexInt = ((Double) indexObject).intValue();
			double diff = (Double) indexObject - indexInt;
			if (diff != 0) {
				throw new RuntimeError(expr.name,
						"Indecies can only be integer values, not double");
			}
			if (indexInt >= list.size() || indexInt < 0) {
				throw new RuntimeError(expr.name,
						"List index out of range.");
			}
			return list.get(indexInt);

		} else if (listObject instanceof String) {
			String string = (String) listObject;
			Object indexObject = evaluate(expr.index);
			if (!(indexObject instanceof Double)) {
				throw new RuntimeError(expr.name,
						"Only numbers can be used as a list index.");
			}
			int indexInt = ((Double) indexObject).intValue();
			double diff = (Double) indexObject - indexInt;
			if (diff != 0) {
				throw new RuntimeError(expr.name,
						"Indecies can only be integer values, not double");
			}
			if (indexInt >= string.length() || indexInt < 0) {
				throw new RuntimeError(expr.name,
						"List index out of range.");
			}
			return Character.toString(string.charAt(indexInt));

		} else {
			throw new RuntimeError(expr.name,
					"Not List or String to access.");
		}
	}

	@Override
	public Object visitListMutatorExpr(Expr.ListMutator expr) {
		Expr.ListAccessor accessor = null;
		if (expr.object instanceof Expr.ListAccessor) {
			accessor = (Expr.ListAccessor) expr.object;
		}
		Object listObject = evaluate(accessor.object);
		if (!(listObject instanceof List)) {
			throw new RuntimeError(expr.name,
					"Not List to mutate by list accessor.");
		}
		List list = (List) listObject;

		Object indexObject = evaluate(accessor.index);
		if (!(indexObject instanceof Double)) {
			throw new RuntimeError(expr.name,
					"Only numbers can be used as a list index.");
		}
		int indexInt = ((Double) indexObject).intValue();
		double diff = (Double) indexObject - indexInt;
		if (diff != 0) {
			throw new RuntimeError(expr.name,
					"Indecies can only be integer values, not double");
		}
		if (indexInt >= list.size() || indexInt < 0) {
			throw new RuntimeError(expr.name,
					"List index out of range.");
		}
		Object value = evaluate(expr.value);
		list.set(indexInt, value);
		return value;
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
	public Object visitQanunListExpr(Expr.QanunList expr) {
		List<Object> list = new ArrayList<>();
		for (Expr expression : expr.list) {
			list.add(evaluate(expression));
		}
		return list;
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
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);
		if (!(object instanceof QanunInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}
		Object value = evaluate(expr.value);
		((QanunInstance) object).set(expr.name, value);
		return value;
	}

	@Override
	public Object visitSuperExpr(Expr.Super expr) {
		int distance = locals.get(expr);
		Token fakeSuperToken = new Token(TokenType.SUPER, "super", null, -1);
		QanunClass superClass = (QanunClass) this.environment.getAt(distance, fakeSuperToken);
		Token fakeThisToken = new Token(TokenType.THIS, "this", null, -1);
		QanunInstance qanunInstance = (QanunInstance) this.environment.getAt(distance - 1, fakeThisToken);
		QanunFunction method = superClass.findMethod(expr.method.getLexeme());
		if (method == null) {
			throw new RuntimeError(expr.method, "Undefined property '" + expr.method.getLexeme() + "'.");
		}
		return method.bind(qanunInstance);
	}

	@Override
	public Object visitThisExpr(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
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

	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
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
	public Void visitClassStmt(Stmt.Class stmt) {
		Object superClass = null;
		if (stmt.superClass != null) {
			superClass = evaluate(stmt.superClass);
			if (!(superClass instanceof QanunClass)) {
				throw new RuntimeError(stmt.superClass.name, "Superclass must be a class.");
			}
		}
		this.environment.define(stmt.name, null);
		if (stmt.superClass != null) {
			this.environment = new Environment(environment);
			Token fakeSuperToken = new Token(TokenType.SUPER, "super", null, -1);
			this.environment.define(fakeSuperToken, superClass);
		}
		Map<String, QanunFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			QanunFunction qanunFunction = new QanunFunction(method, this.environment,
					"init".equals(method.name.getLexeme()));
			methods.put(method.name.getLexeme(), qanunFunction);
		}
		QanunClass qanunClass = new QanunClass(stmt.name.getLexeme(), (QanunClass) superClass, methods);
		if (superClass != null) {
			this.environment = this.environment.getEnclosing();
		}
		environment.assign(stmt.name, qanunClass);
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		Object value = evaluate(stmt.expression);
		if (Qanun.isInRepl) {
			if (stmt.expression instanceof Expr.Call) {
				if (value == null) {
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
		QanunFunction function = new QanunFunction(stmt, this.environment, false);
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
	public Void visitForEachStmt(Stmt.ForEach stmt) {
		execute(stmt.init);
		Object i = environment.get(((Stmt.Var) stmt.init).name);
		if (i != null) {
			throw new RuntimeError(((Stmt.Var) stmt.init).name, "For each iterator can't be explecitly initialized");
		}
		Object iterable = evaluate(stmt.iterable);
		if (iterable instanceof List) {
			for (Object item : (List) iterable) {
				environment.assign(((Stmt.Var) stmt.init).name, item);
				try {
					execute(stmt.body);
				} catch (BreakJump breakJump) {
					break;
				} catch (ContinueJump continueJump) {
					//do nothing just skip the rest of the loop iteration
				}
			}
		}
		if (iterable instanceof String) {
			//environment.define(((Stmt.Var) stmt.init).name, i);
			String string = (String) iterable;
			for (int c = 0; c < string.length(); c++) {
				environment.assign(((Stmt.Var) stmt.init).name, Character.toString(string.charAt(c)));
				try {
					execute(stmt.body);
				} catch (BreakJump breakJump) {
					break;
				} catch (ContinueJump continueJump) {
					//do nothing just skip the rest of the loop iteration
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
				//checkNumberOperands(expr.equalSign, currentValue, value);
				if (value instanceof Double && currentValue instanceof Double) {
					value = (double) currentValue + (double) value;
					break;
				} else if (value instanceof List && currentValue instanceof List) {
					((List) currentValue).addAll((List) value);
					value = currentValue;
					break;
				} else {
					throw new RuntimeError(expr.equalSign, "Operands must be numbers or lists");
				}
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
		//Integer distance = locals.get(expr);
		if (locals.containsKey(expr)) {
			int distance = locals.get(expr);
			environment.assignAt(distance, expr.name, value);
		} else {
			globals.assign(expr.name, value);
		}
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
		return lookUpVariable(expr.name, expr);
	}

	private Object lookUpVariable(Token name, Expr expr) {
		//Integer distance = locals.get(expr);
		if (locals.containsKey(expr)) {
			int distance = locals.get(expr);
			return environment.getAt(distance, name);
		} else {
			return globals.get(name);
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

	String stringify(Object object) {
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
		if (object instanceof List) {
			return object.toString().replaceAll("null", "nil");
		}

		return object.toString();
	}

	private void checkDivisionByZero(Token operator, Object right) {
		if ((double) right == 0.0) {
			throw new RuntimeError(operator, "/ by zero is illegal");
		}
	}

	private void defineGlobals() {
		StandardLibrary.defineGlobals(globals);
	}

}
