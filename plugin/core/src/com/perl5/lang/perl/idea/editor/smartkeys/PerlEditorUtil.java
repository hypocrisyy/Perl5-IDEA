/*
 * Copyright 2015-2020 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.idea.editor.smartkeys;

import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.perl5.lang.perl.lexer.PerlElementTypesGenerated.*;

public class PerlEditorUtil {
  /**
   * Iterates back until atEnd or non-space token
   */
  public static @NotNull HighlighterIterator moveToPreviousMeaningfulToken(@NotNull HighlighterIterator iterator) {
    while (!iterator.atEnd()) {
      IElementType tokenType = iterator.getTokenType();
      if (tokenType != TokenType.WHITE_SPACE) {
        break;
      }
      iterator.retreat();
    }
    return iterator;
  }

  /**
   * Iterates forward until atEnd or non-space token
   */
  public static @NotNull HighlighterIterator moveToNextMeaningfulToken(@NotNull HighlighterIterator iterator) {
    iterator.advance();
    while (!iterator.atEnd()) {
      IElementType tokenType = iterator.getTokenType();
      if (tokenType != TokenType.WHITE_SPACE) {
        break;
      }
      iterator.advance();
    }
    return iterator;
  }

  /**
   * @return previous non-space token type
   */
  public static @Nullable IElementType getPreviousTokenType(@NotNull HighlighterIterator iterator) {
    moveToPreviousMeaningfulToken(iterator);
    return getTokenType(iterator);
  }

  @Contract("null->null")
  public static @Nullable IElementType getTokenType(@Nullable HighlighterIterator iterator) {
    return iterator == null || iterator.atEnd() ? null : iterator.getTokenType();
  }

  public static int getTokenLength(@Nullable HighlighterIterator iterator) {
    return iterator == null || iterator.atEnd() ? -1 : iterator.getEnd() - iterator.getStart();
  }

  @Contract("null->null")
  public static @Nullable CharSequence getTokenChars(@Nullable HighlighterIterator iterator) {
    return iterator == null || iterator.atEnd() ?
           null :
           iterator.getDocument().getCharsSequence().subSequence(iterator.getStart(), iterator.getEnd());
  }

  /**
   * @return next non-space token type; NB: current token is skipped
   */
  public static @Nullable IElementType getNextTokenType(@NotNull HighlighterIterator iterator) {
    moveToNextMeaningfulToken(iterator);
    return getTokenType(iterator);
  }

  /**
   * Returns true if text in editor contains balanced {@code leftType}s and {@code rightType}s for the {@code offset}.
   * Meaning: even number and left is always before the right.
   *
   * @apiNote actually this algorithm is pretty dumb, but, probably, good enough. For the better we should control nesting of braces, brackets and parens.
   * @implSpec we a starting from the beginning and counting open and close markers. If we meet close before open - we fail. If all markers are
   * closed after {@code offset}, we think it's ok.
   */
  public static boolean areMarkersBalanced(@NotNull EditorEx editor,
                                           int offset,
                                           @NotNull IElementType leftType,
                                           @NotNull IElementType rightType) {
    HighlighterIterator highlighterIterator = editor.getHighlighter().createIterator(0);
    int level = 0;
    while (!highlighterIterator.atEnd()) {
      IElementType tokenType = highlighterIterator.getTokenType();
      if (tokenType == leftType) {
        level++;
      }
      else if (tokenType == rightType) {
        level--;
      }
      if (level < 0) {
        return false;
      }
      else if (level == 0 && highlighterIterator.getEnd() > offset) {
        return true;
      }
      highlighterIterator.advance();
    }
    return level == 0;
  }

  public static boolean areMarkersBalanced(@NotNull EditorEx editor, int offset, char openChar) {
    switch (openChar) {
      case '[':
        return areMarkersBalanced(editor, offset, LEFT_BRACKET, RIGHT_BRACKET);
      case '{':
        return areMarkersBalanced(editor, offset, LEFT_BRACE, RIGHT_BRACE);
      case '(':
        return areMarkersBalanced(editor, offset, LEFT_PAREN, RIGHT_PAREN);
    }
    throw new RuntimeException("Incorrect char: " + openChar);
  }
}
