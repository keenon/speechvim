package io.github.keenon.voicecode.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by keenon on 11/26/17.
 *
 * This class is responsible for parsing natural language utterances into corresponding code using a probabilistic
 * context free grammar.
 */
public class SynchronousGrammarParser {
  /**
   * An SLF4J Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SynchronousGrammarParser.class);

  private List<GrammarRule> grammarRules = new ArrayList<>();
  // private List<GrammarRule>
  private Map<String, CKYParser.UnaryRule> inferredUnaryRules = new HashMap<>();
  private CKYParser parser = new CKYParser();

  public SynchronousGrammarParser() {}

  public void addGrammarRules(List<GrammarRule> grammarRules) {
    for (GrammarRule rule : grammarRules) this.addGrammarRule(rule);
  }

  public void addGrammarRule(GrammarRule grammarRule) {
    grammarRules.add(grammarRule);

    if (grammarRule.children.length == 1) {

      // Just add the unary rule, no problem

      parser.addUnaryRules(new CKYParser.UnaryRule(grammarRule.score, grammarRule.parent, grammarRule.allowedAtRoot(), grammarRule.children[0]));
    }
    else if (grammarRule.children.length > 1) {

      // Binarize the rule, left branching

      Optional<Pattern> lastBinaryNonterminal = Optional.empty();
      for (int i = 1; i < grammarRule.children.length; i++) {
        Pattern leftChild = lastBinaryNonterminal.orElse(ensureUnary(grammarRule.children[0]));
        Pattern rightChild = ensureUnary(grammarRule.children[i]);
        String parent;
        if (i == grammarRule.children.length-1) {
          parent = grammarRule.parent;
          parser.addBinaryRules(new CKYParser.BinaryRule(grammarRule.score, parent, grammarRule.allowedAtRoot(), leftChild, rightChild));
        }
        else {
          parent = grammarRule.parent+"::"+i;
          parser.addBinaryRules(new CKYParser.BinaryRule(1.0, parent, grammarRule.allowedAtRoot(), leftChild, rightChild));
          lastBinaryNonterminal = Optional.of(Pattern.compile(parent));
        }
      }
    }
  }

  /**
   * This function ensures that unary symbol is either a terminal token or has a generated unary symbol for it.
   */
  private Pattern ensureUnary(Pattern token) {
    if (token.pattern().toLowerCase().equals(token.pattern())) {
      String output = "_INFERRED_"+token.pattern();
      if (!inferredUnaryRules.containsKey(token.pattern())) {
        CKYParser.UnaryRule rule = new CKYParser.UnaryRule(0.0, output, false, token);
        inferredUnaryRules.put(token.pattern(), rule);
        parser.addUnaryRules(rule);
      }
      Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
      String escapedOutput = SPECIAL_REGEX_CHARS.matcher(output).replaceAll("\\\\$0");
      return Pattern.compile(escapedOutput);
    }
    else return token;
  }

  /**
   * This only returns non-empty if the phrase is in the formal language given by the grammar.
   */
  public Optional<String> translate(List<String> tokens) {
    Optional<CKYParser.TreeNode> optionalTree = parser.parse(tokens);
    if (!optionalTree.isPresent()) return Optional.empty();
    final CKYParser.TreeNode tree = optionalTree.get();
    System.out.println("Got parse tree: "+tree);

    // 4. Map the generated tree back to the original grammar rules.

    CKYParser.TreeNode translated = translateBackFromBinarizedGrammar(tree);

    // 5. Generate the code that the parse tree maps to.

    return Optional.of(generateRecursively(translated, tokens));
  }

  /**
   * This function returns a tree of nodes in the original grammar and not in the binarized version.
   */
  private CKYParser.TreeNode translateBackFromBinarizedGrammar(CKYParser.TreeNode binarizedNode) {

    // Check if this is a binarized node

    if (binarizedNode.children.length == 2) {
      List<CKYParser.TreeNode> realChildren = new ArrayList<>();
      realChildren.add(binarizedNode.children[1]);
      CKYParser.TreeNode checkChild = binarizedNode.children[0];
      while (checkChild.name.contains("::") && checkChild.name.split("::")[0].equals(binarizedNode.name) && checkChild.children.length == 2) {
        realChildren.add(checkChild.children[1]);
        checkChild = checkChild.children[0];
      }
      realChildren.add(checkChild);

      CKYParser.TreeNode[] translatedChildren = new CKYParser.TreeNode[realChildren.size()];
      for (int i = realChildren.size()-1; i >= 0; i--) {
        translatedChildren[realChildren.size() - 1 - i] = translateBackFromBinarizedGrammar(realChildren.get(i));
      }

      return new CKYParser.TreeNode(binarizedNode.nll, binarizedNode.name, binarizedNode.startToken, binarizedNode.endToken, binarizedNode.allowedAtRoot, translatedChildren);
    }
    else if (binarizedNode.children.length == 1) {
      return new CKYParser.TreeNode(binarizedNode.nll, binarizedNode.name, binarizedNode.startToken, binarizedNode.endToken, binarizedNode.allowedAtRoot, translateBackFromBinarizedGrammar(binarizedNode.children[0]));
    }
    else {
      assert(binarizedNode.children.length == 0);
      return binarizedNode;
    }
  }

  /**
   * This checks if the grammar rule could have possibly generated this tree node.
   */
  private boolean ruleMatches(CKYParser.TreeNode node, GrammarRule rule) {
    if (!node.name.equals(rule.parent)) return false;
    // If this is real a unary terminal rule
    if (node.children.length == 0 && rule.children.length == 1 && rule.children[0].pattern().toLowerCase().equals(rule.children[0].pattern())) {
      return node.name.equals(rule.parent);
    }
    if (node.children.length != rule.children.length) return false;
    for (int i = 0; i < node.children.length; i++) {
      // If this rule is a terminal symbol
      if (rule.children[i].pattern().toLowerCase().equals(rule.children[i].pattern())) {
        if (node.children[i].name.contains("_INFERRED_")) {
          String originalToken = node.children[i].name.replace("_INFERRED_", "");
          if (!rule.children[i].pattern().equals(originalToken)) return false;
        }
        else return false;
      }
      else {
        if (!rule.children[i].matcher(node.children[i].name).matches()) return false;
      }
    }
    return true;
  }

  /**
   * This translates from the source parse tree into the target grammar.
   */
  private String generateRecursively(CKYParser.TreeNode translatedNode, List<String> tokens) {
    Optional<GrammarRule> bestGrammarRule = Optional.empty();
    for (GrammarRule rule : grammarRules) {
      if (ruleMatches(translatedNode, rule)) {
        if (!bestGrammarRule.isPresent() || bestGrammarRule.get().score < rule.score) bestGrammarRule = Optional.of(rule);
      }
    }

    if (bestGrammarRule.isPresent()) {
      GrammarRule rule = bestGrammarRule.get();
      String[] childGenerations;
      // Handle unary terminal rules, which map directly to tokens
      if (rule.children.length == 1 && rule.children[0].pattern().toLowerCase().equals(rule.children[0].pattern())) {
        childGenerations = new String[]{tokens.get(translatedNode.startToken)};
      }
      // Handle n-ary nonterminal rules
      else {
        childGenerations = new String[translatedNode.children.length];
        for (int i = 0; i < translatedNode.children.length; i++) {
          childGenerations[i] = generateRecursively(translatedNode.children[i], tokens);
        }
      }
      return rule.generate(childGenerations);
    }
    else {
      if (translatedNode.children.length > 0) throw new IllegalStateException("Got a node that had children but wasn't generated by a grammar rule: "+translatedNode.toString());
      if (translatedNode.startToken != translatedNode.endToken) throw new IllegalStateException("Got a node with no children that spans more than one token: "+translatedNode.toString());
      if (!translatedNode.name.contains("_INFERRED_")) throw new IllegalStateException("Got a unary node with no grammar rule that wasn't an inferred token: "+translatedNode.toString());
      if (translatedNode.startToken < 0 || translatedNode.startToken >= tokens.size()) throw new IllegalStateException("Got a unary inferred node with out-of-bounds source token. tokens.size()="+tokens.size()+", node.startToken="+translatedNode.startToken);
      return tokens.get(translatedNode.startToken);
    }
  }
}
