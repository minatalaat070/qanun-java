/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

import java.util.List;
import java.util.Map;

/**
 *
 * @author mina
 */
public class QanunClass implements QanunCallable {

	final String name;
	final QanunClass superClass;
	private final Map<String, QanunFunction> mathods;

	public QanunClass(String name, QanunClass superClass, Map<String, QanunFunction> methods) {
		this.name = name;
		this.superClass = superClass;
		this.mathods = methods;
	}

	QanunFunction findMethod(String name) {
		if (this.mathods.containsKey(name)) {
			return this.mathods.get(name);
		}
		if (this.superClass != null) {
			return this.superClass.findMethod(name);
		}
		return null;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		QanunInstance qanunInstance = new QanunInstance(this);
		QanunFunction initializer = findMethod("init");
		if (initializer != null) {
			initializer.bind(qanunInstance).call(interpreter, arguments);
		}
		return qanunInstance;
	}

	@Override
	public int arity() {
		QanunFunction initializer = findMethod("init");
		if (initializer == null) {
			return 0;
		}
		return initializer.arity();
	}

	@Override
	public String toString() {
		return name;
	}

}
