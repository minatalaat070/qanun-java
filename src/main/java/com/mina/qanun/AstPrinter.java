package com.mina.qanun;

/**
 *
 * @author mina
 */
public class AstPrinter implements Expr.Visitor<String> {

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return expr.left.toString() + " " + expr.operator.getLexeme() + " " + expr.right.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.right);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    /* a hack for trying the astprinter without parser till end of chapetr 3
    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));

        System.out.println(new AstPrinter().print(expression));
    }
     */
    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        // returns a placeholder value, it needs Environment instance to work as intended 
        return expr.name.toString();
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.name.getLexeme() + " = " + expr.value.toString();
    }

    @Override
    public String visitConditionalTernaryExpr(Expr.ConditionalTernary expr) {
        return expr.condition.toString() + " ? " + expr.trueCondition.toString() + " : " + expr.falseCondition.toString();
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return expr.callee.toString() + " " + expr.paren.getLexeme() + " " + expr.arguments.toString();
    }

    @Override
    public String visitQanunListExpr(Expr.QanunList expr) {
        return expr.list.toString();
    }

    @Override
    public String visitListAccessorExpr(Expr.ListAccessor expr) {
        return expr.name.toString() + "[" + expr.index.toString() + "]";
    }

}
