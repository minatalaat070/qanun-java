/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mina
 */
class QanunInstance {

	private QanunClass qanunClass;
	private final Map<String, Object> fields = new HashMap<>();

	public QanunInstance(QanunClass qanunClass) {
		this.qanunClass = qanunClass;
	}

	Object get(Token name) {
		if (fields.containsKey(name.getLexeme())) {
			return fields.get(name.getLexeme());
		}

		QanunFunction method = qanunClass.findMethod(name.getLexeme());
		if (method != null) {
			return method.bind(this);
		}

		throw new RuntimeError(name, "Undefined property '" + name.getLexeme() + "'.");
	}

	@Override
	public String toString() {
		return qanunClass.name + " instance";
	}

	void set(Token name, Object value) {
		fields.put(name.getLexeme(), value);
	}

}
