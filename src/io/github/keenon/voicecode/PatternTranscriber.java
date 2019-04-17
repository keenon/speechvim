package io.github.keenon.voicecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class PatternTranscriber {
  /**
   * An SLF4J Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PatternTranscriber.class);

  public Optional<String> transcribe(String text) {
    if (text.trim().equals("newline") || text.trim().equals("new line")) {
      return Optional.of("\n");
    }

    String[] tokens = text.trim().split(" ");

    /*
    private static class audio response is
    public class speaking is
    public static void main is
    public static class audio response is
        string text semicolon
        string text equals quote hello world quote semicolon
    boolean intermediate value semicolon
    boolean active equals false semicolon
    */

    // This is a simple class declaration
    StringBuilder sb = new StringBuilder();

    if (tokens[0].equals("public") && tokens[1].equals("class") && tokens[tokens.length-1].equals("is")) {
      sb.append("public class ");
      for (int i = 2; i < tokens.length - 1; i++) {
        appendCapitalized(sb, tokens[i]);
      }
      sb.append(" {\n");
      return Optional.of(sb.toString());
    }
    else if (tokens[0].equals("private") && tokens[1].equals("static") && tokens[2].equals("class") && tokens[tokens.length-1].equals("is")) {
      sb.append("private static class ");
      for (int i = 3; i < tokens.length - 1; i++) {
        appendCapitalized(sb, tokens[i]);
      }
      sb.append(" {\n");
      return Optional.of(sb.toString());
    }

    return Optional.empty();
  }

  private void appendCapitalized(StringBuilder sb, String token) {
    sb.append(token.toUpperCase(), 0, 1);
    sb.append(token.substring(1));
  }
}
