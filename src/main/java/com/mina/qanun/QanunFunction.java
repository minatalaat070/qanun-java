/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

import java.util.List;

/**
 *
 * @author mina
 */
public class QanunFunction implements QanunCallable {

    private final Stmt.Function declaration;
    private final Environment closure;

    public QanunFunction(Stmt.Function declaration,Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(this.closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i), arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.getValue();
        }
        return null;
    }

    @Override
    public String toString() {
        return "<function '" + declaration.name.getLexeme() + "'>";
    }

}
