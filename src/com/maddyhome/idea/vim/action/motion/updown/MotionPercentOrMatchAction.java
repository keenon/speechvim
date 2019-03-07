/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.motion.updown;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.MotionEditorAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.handler.MotionEditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 */
public class MotionPercentOrMatchAction extends MotionEditorAction {
  public MotionPercentOrMatchAction() {
    super(new Handler());
  }

  private static class Handler extends MotionEditorActionHandler {
    @Override
    public int getOffset(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                         int rawCount, @Nullable Argument argument) {
      if (rawCount == 0) {
        return VimPlugin.getMotion().moveCaretToMatchingPair(editor, caret);
      }
      else {
        return VimPlugin.getMotion().moveCaretToLinePercent(editor, count);
      }
    }

    public void process(@NotNull Command cmd) {
      if (cmd.getRawCount() == 0) {
        cmd.setFlags(EnumSet.of(CommandFlags.FLAG_MOT_INCLUSIVE));
      }
      else {
        cmd.setFlags(EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE));
      }
    }
  }
}
