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
public interface QanunCallable {

    public int arity();
    Object call(Interpreter interpreter, List<Object> arguments);

}
