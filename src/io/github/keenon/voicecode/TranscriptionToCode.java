package io.github.keenon.voicecode;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiShortNamesCache;
import io.github.keenon.voicecode.parser.GrammarRule;
import io.github.keenon.voicecode.parser.SynchronousGrammarParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This is responsible for translating the raw transcription text into code.
 */
public class TranscriptionToCode {
  /**
   * An SLF4J Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TranscriptionToCode.class);

  public static String translateTranscription(String transcription, PsiFile psiFile, Project project) {
    return translateTranscription(transcription, Arrays.asList(PsiShortNamesCache.getInstance(project).getAllClassNames()));
  }

  public static String translateTranscription(String transcription, List<String> classNames) {
    Optional<String> code = translateTranscriptionWithGrammar(transcription, classNames);
    if (code.isPresent()) return code.get();

    // Fallback to simple replacements if the parser fails

    String[] parts = transcription.split(" ");
    StringBuilder builder = new StringBuilder();

    for (String part : parts) {
      if (builder.length() > 0 && !"semicolon".equals(part)) builder.append(" ");

      if ("semicolon".equals(part)) {
        builder.append(";");
      }
      else if ("integer".equals(part)) {
        builder.append("int");
      }
      else if ("equals".equals(part) || "equal".equals(part)) {
        builder.append("=");
      }
      else {
        builder.append(part);
      }
    }

    return builder.toString();
  }

  // Google seems to have a lot of trouble recognizing the word "paren", which is annoying, so we're swapping it out for
  // something more phonetically unambiguous
  public static final String PAREN_KEYWORD = "shark";

  /**
   * This does a simple, context-less translation from transcription text to code.
   *
   * @param transcription the raw text from the speech recognition
   * @return a transcribed version
   */
  public static Optional<String> translateTranscriptionWithGrammar(String transcription, List<String> classNames) {
    String[] parts = transcription.split(" ");

    List<String> allTypes = new ArrayList<>();
    // allTypes.addAll(classNames);
    allTypes.add("ArrayList");
    allTypes.add("List");
    allTypes.add("StringBuilder");

    String wildcardTokenPattern = "^(?!call|quote|comma|dot|semicolon|diamond|new|end)[a-z][a-z0-9]*";

    SynchronousGrammarParser parser = new SynchronousGrammarParser();

    // Build all the grammar rules we need

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    for (String type : allTypes) {
      final String finalType = type;
      String ruleName = "CLASS_"+GrammarRule.merge(GrammarRule.splitClassName(type), GrammarRule.GeneratedCase.SNAKE_CASE).toUpperCase();
      parser.addGrammarRule(new GrammarRule(3.0, ruleName, GrammarRule.splitClassName(type)) {
        @Override
        public String generate(String[] childGenerations) {
          return finalType;
        }
      });
      parser.addGrammarRule(GrammarRule.unaryRule(0.0, "CLASS", ruleName));
    }
    */
    parser.addGrammarRules(GrammarRule.matchAnyLengthSequenceOf(-4.0, "CLASS_UNKNOWN", wildcardTokenPattern, GrammarRule.GeneratedCase.CLASS_CASE));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "CLASS", "CLASS_UNKNOWN"));

    parser.addGrammarRule(new GrammarRule(-1.0, "BUILTIN_TYPE", "integer|long|double|float|string|void") {
      @Override
      public String generate(String[] childGenerations) {
        if (childGenerations[0].equals("integer")) return "int";
        if (childGenerations[0].equals("long")) return "long";
        if (childGenerations[0].equals("float")) return "float";
        if (childGenerations[0].equals("double")) return "double";
        if (childGenerations[0].equals("void")) return "void";
        if (childGenerations[0].equals("string")) return "String";
        throw new IllegalStateException("The BUILTIN_TYPE pattern matched an unexpected string: \""+childGenerations[0]+"\"");
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "TYPE", "BUILTIN_TYPE"));

    parser.addGrammarRule(new GrammarRule(1.0, "ARRAY_TYPE", "TYPE", "array") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+"[]";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "TYPE", "ARRAY_TYPE"));

    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "TYPE", "CLASS_OPTIONAL_TEMPLATE"));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Atoms
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRules(GrammarRule.matchAnyLengthSequenceOf(0.0, "VARIABLE", wildcardTokenPattern, GrammarRule.GeneratedCase.CAMEL_CASE));
    parser.addGrammarRules(GrammarRule.matchSequenceWithSeparator(0.0, "DYNAMIC_REF", "EXPRESSION", "dot", "."));
    parser.addGrammarRule(new GrammarRule(3.0, "STATIC_REF", "CLASS", "dot", "DYNAMIC_REF") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+"."+childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "REF", "DYNAMIC_REF"));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "REF", "STATIC_REF"));
    parser.addGrammarRules(GrammarRule.matchAnyLengthSequenceOf(0.0, "STRING:INTERNAL", "[a-z0-9]+", GrammarRule.GeneratedCase.NO_CASE));
    parser.addGrammarRule(new GrammarRule(1.0, "STRING", "quote", "STRING:INTERNAL", "quote") {
      @Override
      public String generate(String[] childGenerations) {
        return "\""+childGenerations[1]+"\"";
      }
    });
    parser.addGrammarRule(new GrammarRule(0.0, "NUMBER", "[0-9]+") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0];
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Expressions
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "NUMBER"));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "STRING"));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "REF"));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "VARIABLE"));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "FUNCTION_CALL"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:PLUS", "EXPRESSION", "plus", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " + " + childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:PLUS"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:TIMES", "EXPRESSION", "times", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " * " + childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:TIMES"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:MINUS", "EXPRESSION", "minus", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " - " + childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:MINUS"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:DIVIDE", "EXPRESSION", "divided", "by", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " / " + childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:DIVIDE"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:PAREN", PAREN_KEYWORD, "EXPRESSION", PAREN_KEYWORD) {
      @Override
      public String generate(String[] childGenerations) {
        return "("+childGenerations[1]+")";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:PAREN"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:LT", "EXPRESSION", "less", "than", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " < " + childGenerations[3];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:LT"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:LTE", "EXPRESSION", "less", "than", "or", "equal", "to", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " <= " + childGenerations[6];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:LTE"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:GT", "EXPRESSION", "greater", "than", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " > " + childGenerations[3];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:GT"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:GTE", "EXPRESSION", "greater", "than", "or", "equal", "to", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " >= " + childGenerations[6];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:GTE"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:EQ", "EXPRESSION", "equals", "equals", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " == " + childGenerations[3];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:EQ"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:AND", "EXPRESSION", "and", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " && " + childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:AND"));

    parser.addGrammarRule(new GrammarRule(1.0, "EXPRESSION:OR", "EXPRESSION", "or", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0] + " || " + childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:OR"));

    parser.addGrammarRule(new GrammarRule(0.0, "EXPRESSION:CLASS_NO_ARGS", "new", "CLASS") {
      @Override
      public String generate(String[] childGenerations) {
        return "new "+childGenerations[1]+"()";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:CLASS_NO_ARGS"));

    parser.addGrammarRule(new GrammarRule(0.0, "EXPRESSION:CLASS_WITH_ARGS", "new", "CLASS", "ARGS") {
      @Override
      public String generate(String[] childGenerations) {
        return "new "+childGenerations[1]+childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:CLASS_WITH_ARGS"));

    parser.addGrammarRule(new GrammarRule(0.0, "EXPRESSION:SIZED_ARRAY_TYPE", "new", "TYPE", "array", "of", "size", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return "new "+childGenerations[1]+"["+childGenerations[5]+"]";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:SIZED_ARRAY_TYPE"));

    parser.addGrammarRule(new GrammarRule(0.0, "EXPRESSION:ARRAY_ACCESS", "EXPRESSION", "array", "element", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+"["+childGenerations[3]+"]";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "EXPRESSION", "EXPRESSION:ARRAY_ACCESS"));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Argument lists
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRules(GrammarRule.matchSequenceWithSeparator(0.0, "ARG_LIST", "EXPRESSION", "comma", ", "));
    parser.addGrammarRule(new GrammarRule(0.0, "ARGS", PAREN_KEYWORD, "ARG_LIST", PAREN_KEYWORD) {
      @Override
      public String generate(String[] childGenerations) {
        return "("+childGenerations[1]+")";
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Type parameter lists
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRules(GrammarRule.matchSequenceWithSeparator(0.0, "TEMPLATE_LIST", "CLASS_OPTIONAL_TEMPLATE", "comma", ", "));
    parser.addGrammarRule(new GrammarRule(0.0, "CLASS_WITH_TEMPLATE", "CLASS", "diamond", "TEMPLATE_LIST", "diamond") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+"<"+childGenerations[2]+">";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "CLASS_OPTIONAL_TEMPLATE", "CLASS"));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "CLASS_OPTIONAL_TEMPLATE", "CLASS_WITH_TEMPLATE"));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Declarations
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "DECLARATION_EXPR", "DECLARATION", "semicolon") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+";";
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    parser.addGrammarRule(new GrammarRule(2.0, "DECLARATION:STATIC", "static", "DECLARATION") {
      @Override
      public String generate(String[] childGenerations) {
        return "static "+childGenerations[1];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "DECLARATION", "DECLARATION:STATIC"));

    parser.addGrammarRule(new GrammarRule(2.0, "DECLARATION:PRIVATE", "private", "DECLARATION") {
      @Override
      public String generate(String[] childGenerations) {
        return "private "+childGenerations[1];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "DECLARATION", "DECLARATION:PRIVATE"));

    parser.addGrammarRule(new GrammarRule(2.0, "DECLARATION:PUBLIC", "public", "DECLARATION") {
      @Override
      public String generate(String[] childGenerations) {
        return "public "+childGenerations[1];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "DECLARATION", "DECLARATION:PUBLIC"));

    parser.addGrammarRule(new GrammarRule(0.0, "DECLARATION:NO_ASSIGNMENT", "TYPE", "VARIABLE") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+" "+childGenerations[1];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "DECLARATION", "DECLARATION:NO_ASSIGNMENT"));

    parser.addGrammarRule(new GrammarRule(0.0, "DECLARATION:REF", "TYPE", "VARIABLE", "equals", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+" "+childGenerations[1]+" = "+childGenerations[3];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "DECLARATION", "DECLARATION:REF"));

    parser.addGrammarRule(new GrammarRule(0.0, "DECLARATION:CLASS_WITH_TEMPLATE_NO_ARGS", "CLASS_WITH_TEMPLATE", "VARIABLE", "equals", "new", "CLASS") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+" "+childGenerations[1]+" = new "+childGenerations[4]+"<>()";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "DECLARATION", "DECLARATION:CLASS_WITH_TEMPLATE_NO_ARGS"));

    parser.addGrammarRule(new GrammarRule(0.0, "DECLARATION:CLASS_WITH_TEMPLATE_WITH_ARGS", "CLASS_WITH_TEMPLATE", "VARIABLE", "equals", "new", "CLASS", "ARGS") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+" "+childGenerations[1]+" = new "+childGenerations[4]+"<>"+childGenerations[5];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "DECLARATION", "DECLARATION:CLASS_WITH_TEMPLATE_WITH_ARGS"));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Assignments
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0,"ASSIGNMENT_EXPR", "ASSIGNMENT", "semicolon") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+";";
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    parser.addGrammarRule(new GrammarRule(0.0, "ASSIGNMENT_EQ", "set", "EXPRESSION", "equals", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+" = "+childGenerations[3];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "ASSIGNMENT", "ASSIGNMENT_EQ"));

    parser.addGrammarRule(new GrammarRule(0.0, "ASSIGNMENT_PLUS_EQ", "set", "EXPRESSION", "plus", "equals", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+" += "+childGenerations[4];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "ASSIGNMENT", "ASSIGNMENT_PLUS_EQ"));

    parser.addGrammarRule(new GrammarRule(0.0, "ASSIGNMENT_MINUS_EQ", "set", "EXPRESSION", "minus", "equals", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+" -= "+childGenerations[4];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "ASSIGNMENT", "ASSIGNMENT_MINUS_EQ"));

    parser.addGrammarRule(new GrammarRule(0.0, "ASSIGNMENT_TIMES_EQ", "set", "EXPRESSION", "times", "equals", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+" *= "+childGenerations[4];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "ASSIGNMENT", "ASSIGNMENT_TIMES_EQ"));

    parser.addGrammarRule(new GrammarRule(0.0, "ASSIGNMENT_DIVIDED_EQ", "set", "EXPRESSION", "divided", "by", "equals", "EXPRESSION") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+" /= "+childGenerations[5];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "ASSIGNMENT", "ASSIGNMENT_DIVIDED_EQ"));

    parser.addGrammarRule(new GrammarRule(0.0, "ASSIGNMENT_PLUS_PLUS", "set", "EXPRESSION", "plus", "plus") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+"++";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "ASSIGNMENT", "ASSIGNMENT_PLUS_PLUS"));

    parser.addGrammarRule(new GrammarRule(0.0, "ASSIGNMENT_MINUS_MINUS", "set", "EXPRESSION", "minus", "minus") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+"--";
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "ASSIGNMENT", "ASSIGNMENT_MINUS_MINUS"));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Function and method calls
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "FUNCTION_CALL_EXPR", "FUNCTION_CALL", "semicolon") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+";";
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    parser.addGrammarRule(new GrammarRule(1.0, "FUNCTION_CALL_NO_ARGS", "call", "REF") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+"()";
      }
    });
    parser.addGrammarRule(new GrammarRule(1.0, "FUNCTION_CALL_ARGS", "call", "REF", "ARGS") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[1]+childGenerations[2];
      }
    });
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "FUNCTION_CALL", "FUNCTION_CALL_NO_ARGS"));
    parser.addGrammarRule(GrammarRule.unaryRule(0.0, "FUNCTION_CALL", "FUNCTION_CALL_ARGS"));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // If statements
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "IF_STMT", "if", "EXPRESSION", "then") {
      @Override
      public String generate(String[] childGenerations) {
        return "if ("+childGenerations[1]+") {";
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // While loop
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "WHILE_LOOP", "while", "EXPRESSION", "do") {
      @Override
      public String generate(String[] childGenerations) {
        return "while ("+childGenerations[1]+") {";
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // For loop
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "FOR_LOOP", "for|4", "DECLARATION", "semicolon", "EXPRESSION", "semicolon", "ASSIGNMENT", "do") {
      @Override
      public String generate(String[] childGenerations) {
        return "for ("+childGenerations[1]+"; "+childGenerations[3]+"; "+childGenerations[5]+") {";
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Class
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "CLASS_EXPR", "CLASS_DEFN", "is") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0];
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    parser.addGrammarRule(new GrammarRule(5.0, "CLASS_DEFN", "class", "CLASS_UNKNOWN") {
      @Override
      public String generate(String[] childGenerations) {
        return "class "+childGenerations[1]+" {";
      }
    });

    parser.addGrammarRule(new GrammarRule(0.0, "CLASS_DEFN", "public", "CLASS_DEFN") {
      @Override
      public String generate(String[] childGenerations) {
        return "public "+childGenerations[1];
      }
    });

    parser.addGrammarRule(new GrammarRule(0.0, "CLASS_DEFN", "private", "CLASS_DEFN") {
      @Override
      public String generate(String[] childGenerations) {
        return "private "+childGenerations[1];
      }
    });

    parser.addGrammarRule(new GrammarRule(0.0, "CLASS_DEFN", "static", "CLASS_DEFN") {
      @Override
      public String generate(String[] childGenerations) {
        return "static "+childGenerations[1];
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Function declaration
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "FUNCTION_DECLARATION", "declare", "function", "FUNCTION_DEFN", "is") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[2];
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    parser.addGrammarRule(new GrammarRule(5.0, "FUNCTION_DEFN", "TYPE", "VARIABLE") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+" "+childGenerations[1]+"() {";
      }
    });

    parser.addGrammarRule(new GrammarRule(5.0, "FUNCTION_DEFN", "TYPE", "VARIABLE", "FUNCTION_ARGS") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+" "+childGenerations[1]+childGenerations[2]+" {";
      }
    });

    parser.addGrammarRule(new GrammarRule(0.0, "FUNCTION_DEFN", "public", "FUNCTION_DEFN") {
      @Override
      public String generate(String[] childGenerations) {
        return "public "+childGenerations[1];
      }
    });

    parser.addGrammarRule(new GrammarRule(0.0, "FUNCTION_DEFN", "private", "FUNCTION_DEFN") {
      @Override
      public String generate(String[] childGenerations) {
        return "private "+childGenerations[1];
      }
    });

    parser.addGrammarRule(new GrammarRule(0.0, "FUNCTION_DEFN", "static", "FUNCTION_DEFN") {
      @Override
      public String generate(String[] childGenerations) {
        return "static "+childGenerations[1];
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Function argument lists
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(0.0, "FUNCTION_ARG", "TYPE", "VARIABLE") {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+" "+childGenerations[1];
      }
    });
    parser.addGrammarRules(GrammarRule.matchSequenceWithSeparator(0.0, "FUNCTION_ARG_LIST", "FUNCTION_ARG", "comma", ", "));
    parser.addGrammarRule(new GrammarRule(0.0, "FUNCTION_ARGS", PAREN_KEYWORD, "FUNCTION_ARG_LIST", PAREN_KEYWORD) {
      @Override
      public String generate(String[] childGenerations) {
        return "("+childGenerations[1]+")";
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // End
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(1.0, "END", "end") {
      @Override
      public String generate(String[] childGenerations) {
        return "}";
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Comment
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    parser.addGrammarRule(new GrammarRule(15.0, "COMMENT", "comment", "COMMENT:INTERNAL") {
      @Override
      public String generate(String[] childGenerations) {
        return "// "+childGenerations[1];
      }

      @Override
      public boolean allowedAtRoot() {
        return true;
      }
    });

    parser.addGrammarRules(GrammarRule.matchAnyLengthSequenceOf(-10.0, "COMMENT:INTERNAL", ".+", GrammarRule.GeneratedCase.NO_CASE));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Actually run the parser
    //

    Optional<String> attemptedParse = parser.translate(Arrays.asList(parts));
    return attemptedParse;
  }
}
