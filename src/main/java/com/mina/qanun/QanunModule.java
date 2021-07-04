/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

import java.util.Map;

/**
 *
 * @author mina
 */
public class QanunModule {

	private final Token name;
	private final Map<String, Stmt.Class> classes;
	private final Map<String, Stmt.Function> functions;
	private final Map<String, Stmt.Var> variables;
	private final Map<String, Stmt.Val> constants;
	private final Environment environment;

	public QanunModule(Token name, Map<String, Stmt.Class> classes, Map<String, Stmt.Function> functions,
			Map<String, Stmt.Var> variables, Map<String, Stmt.Val> constants,Environment environment) {
		this.name = name;
		this.classes = classes;
		this.functions = functions;
		this.variables = variables;
		this.constants = constants;
		this.environment = environment;
	}

	Object get(Token name) {
		if (classes.containsKey(name.getLexeme())) {
			return environment.get(name);
		}
		if (functions.containsKey(name.getLexeme())) {
			return environment.get(name);
		}
		if (variables.containsKey(name.getLexeme())) {
			return environment.get(name);
		}
		if (constants.containsKey(name.getLexeme())) {
			return environment.get(name);
		}
		return null;
	}

	@Override
	public String toString() {
		return "Module : '" + this.name.getLexeme() + "'";
	}

}
