package com.mina.qanun;

import java.util.List;

/**
 *
 * @author mina
 */
public interface QanunCallable {

	public int arity();

	Object call(Interpreter interpreter, List<Object> arguments);

}
