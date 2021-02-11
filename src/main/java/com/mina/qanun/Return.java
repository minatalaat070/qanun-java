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
