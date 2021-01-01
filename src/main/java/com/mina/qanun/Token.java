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
public class Token {

    private final TokenType type;
    private final String lexeme;
    private final Object literal;
    private final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public Object getLiteral() {
        return literal;
    }

    public int getLine() {
        return line;
    }
    
    @Override
    public String toString() {
        return "[ Type: "+type + ", Lexeme: " + lexeme + ", Literal: " + literal+" ]";
    }

}
