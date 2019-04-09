package io.github.keenon.voicecode.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by keenon on 11/26/17.
 *
 * Rules: capitalized strings are nonterminals, lowercase is terminal. "*" is for wildcard terminal symbols.
 */
public abstract class GrammarRule {
  /**
   * An SLF4J Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(GrammarRule.class);

  public double score;
  public String parent;
  public Pattern[] children;

  private static Pattern[] toPatterns(String[] children) {
    Pattern[] patterns = new Pattern[children.length];
    for (int i = 0; i < children.length; i++) patterns[i] = Pattern.compile(children[i]);
    return patterns;
  }

  public GrammarRule(double score, String parent, String... children) {
    this(score, parent, toPatterns(children));
  }

  public GrammarRule(double score, String parent, Pattern... children) {
    this.score = score;
    this.parent = parent;
    this.children = children;

    if (this.parent.toLowerCase().equals(this.parent)) {
      throw new IllegalStateException("Cannot have a GrammarRule with a terminal as a parent: " + this.parent);
    }
    if (this.parent.contains("::")) {
      throw new IllegalStateException("\"::\" is a reserved sequence in nonterminal symbols, but parent is: " + this.parent);
    }
    for (int i = 0; i < children.length; i++) {
      if (this.children[i].pattern().contains("::") && !this.children[i].pattern().equals(this.children[i])) {
        throw new IllegalStateException("\"::\" is a reserved sequence in nonterminal symbols, but child["+i+"] is: " + this.children[i].pattern());
      }
    }
  }

  /**
   * We only allow grammar rules that have this set to true to be root nodes in a parse
   */
  public boolean allowedAtRoot() {
    return false;
  }

  public abstract String generate(String[] childGenerations);

  public enum GeneratedCase {
    NO_CASE,
    CAMEL_CASE,
    CLASS_CASE,
    SNAKE_CASE
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // The grammar generation methods
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * This is just simple syntactic sugar for defining grammars
   */
  public static GrammarRule unaryRule(double score, String parent, String child) {
    return new GrammarRule(score, parent, child) {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0];
      }
    };
  }

  /**
   * This handles matching variable length sequences of wild cards
   */
  public static List<GrammarRule> matchAnyLengthSequenceOf(double score, String parent, String symbol, GeneratedCase generatedCase) {
    List<GrammarRule> grammarRules = new ArrayList<>();

    // 1. The single-token case

    grammarRules.add(new GrammarRule(score, parent, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return merge(childGenerations, generatedCase);
      }
    });

    // 2. The two-token case

    grammarRules.add(new GrammarRule(score, parent, symbol, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return merge(childGenerations, generatedCase);
      }
    });

    // 3. The n-token case

    // 3.1. The cap rule

    grammarRules.add(new GrammarRule(score, parent, ":"+parent+":REPEATED", symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return merge(splitOnSpaces(childGenerations), generatedCase);
      }
    });

    // 3.2. The internal repeat rule

    grammarRules.add(new GrammarRule(0.0, ":"+parent+":REPEATED", ":"+parent+":REPEATED", symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return joinWithSpaces(splitOnSpaces(childGenerations));
      }
    });

    // 3.3. The starting repeat rule

    grammarRules.add(new GrammarRule(0.0, ":"+parent+":REPEATED", symbol, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return joinWithSpaces(childGenerations);
      }
    });

    return grammarRules;
  }

  /**
   * This handles matching variable length sequences of symbols, separated by a given separator
   */
  public static List<GrammarRule> matchSequenceWithSeparator(double score, String parent, String symbol, String separatorSymbol, String separator) {
    List<GrammarRule> grammarRules = new ArrayList<>();

    // 1. The single-token case

    grammarRules.add(new GrammarRule(score, parent, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0];
      }
    });

    // 2. The two-token case

    grammarRules.add(new GrammarRule(score + 0.1, parent, symbol, separatorSymbol, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+separator+childGenerations[2];
      }
    });

    // 3. The n-token case

    // 3.1. The cap rule

    grammarRules.add(new GrammarRule(score, parent, ":"+parent+":REPEATED", separatorSymbol, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+separator+childGenerations[2];
      }
    });

    // 3.2. The internal repeat rule

    grammarRules.add(new GrammarRule(0.1, ":"+parent+":REPEATED", ":"+parent+":REPEATED", separatorSymbol, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+separator+childGenerations[2];
      }
    });

    // 3.3. The starting repeat rule

    grammarRules.add(new GrammarRule(0.1, ":"+parent+":REPEATED", symbol, separatorSymbol, symbol) {
      @Override
      public String generate(String[] childGenerations) {
        return childGenerations[0]+separator+childGenerations[2];
      }
    });

    return grammarRules;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // The utility methods
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static String[] splitClassName(String className) {
    char[] chars = className.toCharArray();
    int lastSplitPoint = 0;
    List<String> pieces = new ArrayList<>();

    for (int i = 1; i < chars.length; i++) {
      if (Character.isUpperCase(chars[i])) {
        pieces.add(className.substring(lastSplitPoint, i).toLowerCase());
        lastSplitPoint = i;
      }
    }
    if (lastSplitPoint < chars.length-1) {
      pieces.add(className.substring(lastSplitPoint).toLowerCase());
    }

    return pieces.toArray(new String[pieces.size()]);
  }

  public static String joinWithSpaces(String[] children) {
    StringBuilder sb = new StringBuilder();
    for (String child : children) {
      if (sb.length() > 0) sb.append(" ");
      sb.append(child);
    }
    return sb.toString();
  }

  public static String[] splitOnSpaces(String[] children) {
    List<String> split = new ArrayList<>();
    for (String child : children) {
      split.addAll(Arrays.asList(child.split(" ")));
    }
    return split.toArray(new String[split.size()]);
  }

  public static String capitalizeFirstLetter(String token) {
    return token.substring(0,1).toUpperCase() + token.substring(1).toLowerCase();
  }

  public static String merge(String[] children, String separator) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < children.length; i++) {
      if (i > 0) sb.append(separator).append(" ");
      sb.append(children[i]);
    }

    return sb.toString();
  }

  public static String merge(String[] children, GeneratedCase generatedCase) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < children.length; i++) {
      if (generatedCase == GeneratedCase.CAMEL_CASE) {
        if (i == 0) sb.append(children[i].toLowerCase());
        else sb.append(capitalizeFirstLetter(children[i]));
      }
      else if (generatedCase == GeneratedCase.CLASS_CASE) {
        sb.append(capitalizeFirstLetter(children[i]));
      }
      else if (generatedCase == GeneratedCase.SNAKE_CASE) {
        if (i > 0) sb.append("_");
        sb.append(children[i].toLowerCase());
      }
      else if (generatedCase == GeneratedCase.NO_CASE) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(children[i].toLowerCase());
      }
      else {
        throw new IllegalStateException("Unexpected case to generate a token with: "+generatedCase);
      }
    }

    return sb.toString();
  }
}
