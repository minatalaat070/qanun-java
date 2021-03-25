package com.mina.qanun;

/**
 *
 * @author mina
 */
public class Return extends RuntimeException {

	private final Object value;

	Return(Object value) {
		super(null, null, false, false);
		this.value = value;
	}

	Object getValue() {
		return this.value;
	}
}
