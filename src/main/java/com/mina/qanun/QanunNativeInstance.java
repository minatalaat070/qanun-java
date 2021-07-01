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
public class QanunNativeInstance implements QanunCallable {

	private final String name;
	private final Map<String, QanunCallable> methods;

	public QanunNativeInstance(String name, Map<String, QanunCallable> methods) {
		this.name = name;
		this.methods = methods;
	}

	QanunCallable findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}
		return null;
	}

	@Override
	public int arity() {
		QanunCallable init = methods.get("init");
		if (init == null) {
			return 0;
		}
		return init.arity();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		QanunCallable initializer = methods.get("init");
		if (initializer != null) {
			initializer.call(interpreter, arguments);
		}
		return this;
	}

	public String getName() {
		return name;
	}
	
}
