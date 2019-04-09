package io.github.keenon.voicecode.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by keenon on 11/26/17.
 */
public class CKYParser {
  /**
   * An SLF4J Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(CKYParser.class);

  private List<UnaryRule> unaryTerminalRules = new ArrayList<>();
  private List<UnaryRule> unaryNonterminalRules = new ArrayList<>();
  private List<BinaryRule> binaryRules = new ArrayList<>();

  public static class UnaryRule {
    double score;
    String output;
    Pattern input;
    boolean allowedAtRoot;

    public UnaryRule(double score, String output, boolean allowedAtRoot, String input) {
      this(score, output, allowedAtRoot, Pattern.compile(input));
    }

    public UnaryRule(double score, String output, boolean allowedAtRoot, Pattern input) {
      this.score = score;
      this.output = output;
      this.input = input;
      this.allowedAtRoot = allowedAtRoot;
    }
  }

  public static class BinaryRule {
    double nll;
    String output;
    Pattern inputLeft;
    Pattern inputRight;
    boolean allowedAtRoot;

    public BinaryRule(double nll, String output, boolean allowedAtRoot, String inputLeft, String inputRight) {
      this(nll, output, allowedAtRoot, Pattern.compile(inputLeft), Pattern.compile(inputRight));
    }

    public BinaryRule(double nll, String output, boolean allowedAtRoot, Pattern inputLeft, Pattern inputRight) {
      this.nll = nll;
      this.output = output;
      this.inputLeft = inputLeft;
      this.inputRight = inputRight;
      this.allowedAtRoot = allowedAtRoot;
    }
  }

  public void addUnaryRules(UnaryRule unaryRule) {
    if (unaryRule.input.pattern().toLowerCase().equals(unaryRule.input.pattern())) {
      this.unaryTerminalRules.add(unaryRule);
    }
    else {
      this.unaryNonterminalRules.add(unaryRule);
    }
  }

  public void addBinaryRules(BinaryRule binaryRule) {
    this.binaryRules.add(binaryRule);
  }

  public static class TreeNode {
    double nll;
    String name;
    int startToken;
    int endToken;
    boolean allowedAtRoot;
    TreeNode[] children;

    public TreeNode(double nll, String name, int startToken, int endToken, boolean allowedAtRoot, TreeNode... children) {
      this.nll = nll;
      this.name = name;
      this.startToken = startToken;
      this.endToken = endToken;
      this.allowedAtRoot = allowedAtRoot;
      this.children = children;
    }

    /**
     * For unit tests.
     */
    public boolean deepEquals(TreeNode otherTree) {
      if (otherTree == null) return false;
      if (!name.equals(otherTree.name)) return false;
      if (allowedAtRoot != otherTree.allowedAtRoot) return false;
      if (children.length != otherTree.children.length) return false;
      if (startToken != otherTree.startToken || endToken != otherTree.endToken) return false;
      for (int i = 0; i < children.length; i++) {
        if (!children[i].deepEquals(otherTree.children[i])) return false;
      }
      return true;
    }

    /**
     * This is just for unit testing.
     */
    @Override
    public String toString() {
      return "\n"+toString(0);
    }

    private String toString(int indentation) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < indentation; i++) sb.append("\t");
      sb.append("[").append(name).append("]");
      if (children.length > 0) {
        sb.append("(\n");
        for (TreeNode node : children) {
          sb.append(node.toString(indentation+1));
          sb.append("\n");
        }
        for (int i = 0; i < indentation; i++) sb.append("\t");
        sb.append(")");
      }
      else {
        assert(startToken == endToken);
        sb.append("(tok="+startToken+")");
      }
      return sb.toString();
    }
  }

  /**
   * Handles the TreeNode list in a cell on the CKY chart.
   */
  public static class ChartCell {
    Map<String, TreeNode> nodes = new HashMap<>();

    /**
     * Adds this node if it's the highest scoring way to get to the provided non-terminal symbol.
     */
    public void addNode(TreeNode node) {

      // If there's already a node here, only replace if the score is better

      if (nodes.containsKey(node.name)) {
        TreeNode oldNode = nodes.get(node.name);
        if (node.nll > oldNode.nll) {
          nodes.put(node.name, node);
        }
      }

      // Otherwise always insert

      else {
        nodes.put(node.name, node);
      }
    }

    /**
     * This adds all the unary non-terminal rules we need.
     */
    public void processUnaryNonTerminals(List<UnaryRule> unaryRules) {
      Collection<TreeNode> nodesToProcess = nodes.values();

      while (true) {
        List<TreeNode> nodesToAdd = new ArrayList<>();

        for (TreeNode node : nodesToProcess) {
          for (UnaryRule unaryRule : unaryRules) {
            if (unaryRule.input.matcher(node.name).matches()) {
              if (!checkUnaryTailForDuplicates(node, unaryRule.output)) {
                nodesToAdd.add(new TreeNode(node.nll + unaryRule.score, unaryRule.output, node.startToken, node.endToken, unaryRule.allowedAtRoot, node));
              }
            }
          }
        }

        if (nodesToAdd.size() == 0) break;

        for (TreeNode node : nodesToAdd) addNode(node);
        nodesToProcess = nodesToAdd;
      }
    }

    /**
     * Gets the highest scoring node in this chart cell
     */
    public Optional<TreeNode> highestScoringNode() {
      Optional<TreeNode> maxNode = Optional.empty();
      for (TreeNode node : nodes.values()) {
        if (!node.allowedAtRoot) continue;
        if (!maxNode.isPresent() || node.nll > maxNode.get().nll) {
          maxNode = Optional.of(node);
        }
      }
      return maxNode;
    }

    public Collection<TreeNode> nodes() {
      return nodes.values();
    }

    private static boolean checkUnaryTailForDuplicates(TreeNode node, String tail) {
      if (node.name.equals(tail)) return true;
      if (node.children.length == 1) return checkUnaryTailForDuplicates(node.children[0], tail);
      return false;
    }
  }

  /**
   * Runs CKY parsing.
   */
  public Optional<TreeNode> parse(List<String> tokens) {
    ChartCell[][] table = new ChartCell[tokens.size()][tokens.size()];

    // 1. Make all the unary generations

    for (int i = 0; i < tokens.size(); i++) {
      table[i][i] = new ChartCell();

      for (UnaryRule unaryRule : unaryTerminalRules) {
        if (unaryRule.input.matcher(tokens.get(i).toLowerCase()).matches()) {
          table[i][i].addNode(new TreeNode(unaryRule.score, unaryRule.output, i, i, unaryRule.allowedAtRoot));
        }
      }

      table[i][i].processUnaryNonTerminals(unaryNonterminalRules);
    }

    // 2. Make all the binary generations

    for (int size = 1; size < tokens.size(); size++) {
      for (int left = 0; left < tokens.size() - size; left++) {
        int right = left + size;
        table[left][right] = new ChartCell();
        for (int partition = 0; partition < size; partition++) {
          for (TreeNode leftNode : table[left][left + partition].nodes()) {
            for (TreeNode rightNode : table[left + partition + 1][right].nodes()) {
              for (BinaryRule binaryRule : binaryRules) {
                if (binaryRule.inputLeft.matcher(leftNode.name).matches() && binaryRule.inputRight.matcher(rightNode.name).matches()) {
                  double nll = leftNode.nll + rightNode.nll + binaryRule.nll;
                  table[left][right].addNode(new TreeNode(nll, binaryRule.output, left, right, binaryRule.allowedAtRoot, leftNode, rightNode));
                }
              }
            }
          }
        }
        table[left][right].processUnaryNonTerminals(unaryNonterminalRules);
      }
    }

    // 3. Find the highest scoring parent node that covers the whole chart

    return table[0][tokens.size()-1].highestScoringNode();
  }
}
