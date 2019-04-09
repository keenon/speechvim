package io.github.keenon.voicecode;

import com.intellij.formatting.Indent;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by keenon on 12/4/17.
 */
public class IntilliJUtils {
  /**
   * An SLF4J Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(IntilliJUtils.class);

  /**
   * Returns a count of the number of indentations at the beginning of the currently selected line
   */
  static int getIndentsAtCursor(Editor editor, Document document) {
    Caret caret = editor.getCaretModel().getPrimaryCaret();
    int lineNumber = caret.getLogicalPosition().line;
    if (lineNumber >= document.getLineCount()) {
      System.out.println("Line number ("+lineNumber+") is OOB for document line count ("+document.getLineCount()+")");
      return 0;
    }

    int start = document.getLineStartOffset(lineNumber);
    int end = document.getLineEndOffset(lineNumber);

    String lineContents = document.getText(TextRange.from(start, end - start));
    System.out.println(lineContents);

    int indents = 0;
    StringBuilder indentText = new StringBuilder();
    while (true) {
      if (!lineContents.startsWith(indentText.toString())) return indents;

      indents++;
      System.out.println("Indent: \""+ Indent.getNormalIndent().getType().toString()+"\"");
      // editor.getIndentsModel().getCaretIndentGuide().indentLevel;
      indentText.append("\t");
    }
  }
}
