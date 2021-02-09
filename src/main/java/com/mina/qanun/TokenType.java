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
public enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET,
    COMMA, DOT, SEMICOLON,
    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    MINUS, PLUS,
    PLUS_PLUS, PLUS_EQUAL,
    MINUS_MINUS, MINUS_EQUAL,
    STAR, STAR_STAR, STAR_EQUAL, STAR_STAR_EQUAL,
    SLASH, SLASH_EQUAL,
    PERCENTAGE, PERCENTAGE_EQUAL, QUESTION_MARK, COLON,
    // Literals.
    IDENTIFIER, STRING, NUMBER,
    // Keywords.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, VAL, WHILE, SWITCH,
    EOF
}
