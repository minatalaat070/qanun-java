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
public class QanunClass extends QanunInstance implements QanunCallable {

	final String name;
	final QanunClass superClass;
	private final Map<String, QanunFunction> methods;

	public QanunClass(QanunClass metaClass, String name, QanunClass superClass, Map<String, QanunFunction> methods) {
		super(metaClass);
		this.name = name;
		this.superClass = superClass;
		this.methods = methods;
	}

	QanunFunction findMethod(QanunInstance qanunInstance, String name) {
		if (this.methods.containsKey(name)) {
			return this.methods.get(name).bind(qanunInstance);
		}
		if (this.superClass != null) {
			return this.superClass.findMethod(qanunInstance, name);
		}
		return null;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		QanunInstance qanunInstance = new QanunInstance(this);
		QanunFunction initializer = methods.get("init");
		if (initializer != null) {
			initializer.bind(qanunInstance).call(interpreter, arguments);
		}
		return qanunInstance;
	}

	@Override
	public int arity() {
		QanunFunction initializer = methods.get("init");
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
