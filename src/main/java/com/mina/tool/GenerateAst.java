/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mina.tool;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Arrays;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author mina
 */
public class GenerateAst {

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Logical  : Expr left, Token operator, Expr right",
                "Unary    : Token operator, Expr right",
                "Variable : Token name",
                "ConditionalTernary: Expr condition, Expr trueCondition, Expr falseCondition"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression", //expression statment
                "If         : Expr condition, Stmt thenBranch,"
                + " Stmt elseBranch",
                "Print      : Expr expression", // print statement
                "Var        : Token name, Expr initializer",
                "Val        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body",
                "For        : Stmt init, Expr condition, Expr increment, Stmt body",
                "Break      : Token name",
                "Continue   : Token name"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> exprTypes) throws FileNotFoundException, UnsupportedEncodingException {
        String path = outputDir + "/" + baseName + ".java";
        try ( PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("package com.mina.qanun;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("abstract class " + baseName + " {");
            defineVisitor(writer, baseName, exprTypes);
            for (String type : exprTypes) {
                String className = type.split(":")[0].trim();
                String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }
            // The base accept() method.
            writer.println();
            writer.println("  abstract <R> R accept(Visitor<R> visitor);");
            writer.println("}");
        }
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("  static class " + className + " extends " + baseName + " {");
        // Constructor.
        writer.println("    " + className + "(" + fieldList + ") {");
        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }
        writer.println("    }");
        // Visitor pattern.
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit"
                + className + baseName + "(this);");
        writer.println("    }");

        // Fields.
        writer.println();
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println("  }");

    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> exprTypes) {
        writer.println("  interface Visitor<R> {");
        for (String type : exprTypes) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "("
                    + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("  }");
    }

}
