/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.qanun;

/**
 *
 * @author mina
 */
public class RuntimeError extends RuntimeException{
    final Token token;

    public RuntimeError(Token token,String message) {
        super(message);
        this.token = token;
    }
    
}
