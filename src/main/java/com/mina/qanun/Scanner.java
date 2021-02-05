package com.mina.qanun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mina.qanun.TokenType;

/**
 *
 * @author mina
 */
public class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int openParen = 0;
    private int openSquareBrackets = 0;
    private static final Map<String, TokenType> keywords;
    private boolean isEOLinComment = false;

    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("fun", TokenType.FUN);
        keywords.put("if", TokenType.IF);
        keywords.put("nil", TokenType.NIL);
        keywords.put("or", TokenType.OR);
        keywords.put("print", TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super", TokenType.SUPER);
        keywords.put("this", TokenType.THIS);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);
        keywords.put("swich", TokenType.SWITCH);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(TokenType.LEFT_PAREN);
                openParen++;
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                openParen--;
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                // adding implicit semicolon before right brace if not found
                // fixes the issue of one line block
                if (tokens.get(tokens.size() - 1).getType() != TokenType.SEMICOLON) {
                    addToken(TokenType.SEMICOLON);
                }
                addToken(TokenType.RIGHT_BRACE);
                break;
            case '[':
                addToken(TokenType.LEFT_SQUARE_BRACKET);
                openSquareBrackets++;
                break;
            case ']':
                addToken(TokenType.RIGHT_SQUARE_BRACKET);
                openSquareBrackets--;
                break;
            case ',':
                addToken(TokenType.COMMA);
                break;
            case '.':
                addToken(TokenType.DOT);
                break;
            case '-':
                addToken(isNextMatches('-') ? TokenType.MINUS_MINUS : isNextMatches('=') ? TokenType.MINUS_EQUAL : TokenType.MINUS);
                break;
            case '+':
                addToken(isNextMatches('+') ? TokenType.PLUS_PLUS : isNextMatches('=') ? TokenType.PLUS_EQUAL : TokenType.PLUS);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '*':
                addToken(isNextMatches('*')
                        ? isNextMatches('=')
                        ? TokenType.STAR_STAR_EQUAL : TokenType.STAR_STAR : isNextMatches('=')
                        ? TokenType.STAR_EQUAL : TokenType.STAR);
                break;
            case '/':
                if (isNextMatches('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                    if (peek() == '\n') {
                        isEOLinComment = true;
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case '%':
                addToken(isNextMatches('=') ? TokenType.PERCENTAGE_EQUAL : TokenType.PERCENTAGE);
                break;
            case '!':
                addToken(isNextMatches('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(isNextMatches('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(isNextMatches('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(isNextMatches('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '\n':
                ++line;
                if (isEOLinComment) {
                    isEOLinComment = false;
                    break;
                } else if (peekNext() == '\0') {
                    if (tokens.get(tokens.size() - 1).getType() != TokenType.SEMICOLON
                            && tokens.get(tokens.size() - 1).getType() != TokenType.RIGHT_BRACE) {
                        // adding implicit semicolon in case of the script is one line 
                        // for example when input is equivelent to ```print \"Hello\"\nEOF```
                        // with checking if the last symbol is semecolon and if not add implicit one
                        addToken(TokenType.SEMICOLON);
                    }
                    break;
                }
                Token lastToken = tokens.get(tokens.size() - 1);
                if (openParen == 0 && openSquareBrackets == 0
                        && lastToken.getType() != TokenType.SEMICOLON
                        && lastToken.getType() != TokenType.LEFT_BRACE
                        && lastToken.getType() != TokenType.RIGHT_BRACE) {
                    addToken(TokenType.SEMICOLON);
                }
                break;
            case '\r':
            case ' ':
            case '\t':
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Qanun.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(TokenType.NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Qanun.error(line, "Unterminated string.");
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean isNextMatches(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        // it can be replaced with return source.charAt(current++);
        current++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        if (text.equals("\n")) {
            text = "\\n";
        }
        tokens.add(new Token(type, text, literal, line));
    }

}
