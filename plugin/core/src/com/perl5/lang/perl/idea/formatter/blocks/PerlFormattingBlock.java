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

package com.perl5.lang.perl.idea.formatter.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import com.perl5.lang.perl.idea.formatter.PerlFormattingContext;
import com.perl5.lang.perl.lexer.PerlElementTypes;
import com.perl5.lang.perl.parser.PerlParserUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.perl5.lang.perl.lexer.PerlTokenSets.HEREDOC_BODIES_TOKENSET;
import static com.perl5.lang.perl.lexer.PerlTokenSets.MATCH_REGEXP_CONTAINERS;


public class PerlFormattingBlock extends AbstractBlock implements PerlElementTypes, PerlAstBlock {
  /**
   * Composite elements that should be treated as leaf elements, no children
   */
  public static final TokenSet LEAF_ELEMENTS =
    TokenSet.create(
      POD,
      PerlParserUtil.DUMMY_BLOCK
    );

  protected final @NotNull PerlFormattingContext myContext;
  private Indent myIndent;
  private Boolean myIsFirst;
  private Boolean myIsLast;
  private Boolean myIsIncomple;
  private final AtomicNotNullLazyValue<List<Block>> mySubBlocksProvider = AtomicNotNullLazyValue.createValue(
    () -> ContainerUtil.immutableList(buildSubBlocks())
  );

  public PerlFormattingBlock(@NotNull ASTNode node, @NotNull PerlFormattingContext context) {
    super(context.registerNode(node), context.getWrap(node), context.getAlignment(node));
    myContext = context;
    buildChildren();
    myIndent = context.getIndentProcessor().getNodeIndent(node);
  }

  @Override
  public void setIndent(@Nullable Indent indent) {
    myIndent = indent;
  }

  protected final @NotNull PerlFormattingContext getContext() {
    return myContext;
  }

  @Override
  protected final @NotNull List<Block> buildChildren() {
    if (isLeaf()) {
      return Collections.emptyList();
    }
    return mySubBlocksProvider.getValue();
  }

  protected @NotNull List<Block> buildSubBlocks() {
    if (MATCH_REGEXP_CONTAINERS.contains(getElementType())) {
      return buildRegexpSubBlocks();
    }

    List<Block> blocks = new ArrayList<>();

    for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (shouldCreateSubBlockFor(child)) {
        blocks.add(createBlock(child));
      }
    }
    return processSubBlocks(blocks);
  }

  protected @NotNull List<Block> buildRegexpSubBlocks() {
    ASTNode run = myNode.getFirstChildNode();
    if (run == null) {
      return Collections.emptyList();
    }

    List<Block> result = new ArrayList<>();
    int startOffset = -1;
    while (run != null) {
      IElementType elementType = PsiUtilCore.getElementType(run);
      if (elementType == TokenType.WHITE_SPACE || elementType == REGEX_TOKEN || elementType == COMMENT_LINE) {
        if (startOffset < 0) {
          startOffset = run.getStartOffset();
        }
      }
      else {
        if (startOffset >= 0) {
          result.add(new PerlTextRangeBasedBlock(TextRange.create(startOffset, run.getStartOffset())));
          startOffset = -1;
        }
        if (shouldCreateSubBlockFor(run)) {
          result.add(createBlock(run));
        }
      }
      run = run.getTreeNext();
    }

    if (startOffset >= 0) {
      result.add(new PerlTextRangeBasedBlock(TextRange.create(startOffset, myNode.getStartOffset() + myNode.getTextLength())));
    }

    return result;
  }

  private @NotNull List<Block> processSubBlocks(@NotNull List<Block> rawBlocks) {
    IElementType elementType = getElementType();
    if (elementType == SIGNATURE_ELEMENT && rawBlocks.size() == 1) {
      while (rawBlocks.size() == 1) {
        rawBlocks = rawBlocks.get(0).getSubBlocks();
      }
      return rawBlocks;
    }
    else if (elementType == COMMA_SEQUENCE_EXPR) {
      return processCommaSequenceBlocks(rawBlocks);
    }
    return rawBlocks;
  }

  private @NotNull List<Block> processCommaSequenceBlocks(@NotNull List<Block> rawBlocks) {
    List<Block> result = new ArrayList<>();
    List<Block> blocksToGroup = new ArrayList<>();
    boolean[] hasFatComma = new boolean[]{false};
    Runnable blocksDispatcher = () -> {
      if (blocksToGroup.isEmpty()) {
        return;
      }
      if (hasFatComma[0]) {
        result.add(new PerlSyntheticBlock(this, blocksToGroup, null, null, myContext));
        hasFatComma[0] = false;
      }
      else {
        result.addAll(blocksToGroup);
      }
      blocksToGroup.clear();
    };

    for (Block block : rawBlocks) {
      IElementType blockType = block instanceof ASTBlock ? PsiUtilCore.getElementType(((ASTBlock)block).getNode()) : null;
      if ((blockType == null || blockType == COMMA)) {
        blocksToGroup.add(block);
        blocksDispatcher.run();
      }
      else if (blocksToGroup.isEmpty() && (blockType == COMMENT_LINE || blockType == COMMENT_ANNOTATION)) {
        result.add(block);
      }
      else {
        blocksToGroup.add(block);
        hasFatComma[0] |= blockType == FAT_COMMA;
      }
    }
    blocksDispatcher.run();

    return result;
  }

  protected PerlFormattingBlock createBlock(@NotNull ASTNode node) {
    if (HEREDOC_BODIES_TOKENSET.contains(PsiUtilCore.getElementType(node))) {
      return new PerlHeredocFormattingBlock(node, myContext);
    }
    return new PerlFormattingBlock(node, myContext);
  }

  @Override
  public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
    return myContext.getSpacing(this, child1, child2);
  }

  @Override
  public boolean isLeaf() {
    return myNode.getFirstChildNode() == null || LEAF_ELEMENTS.contains(myNode.getElementType());
  }

  @Override
  public Indent getIndent() {
    return myIndent;
  }

  @Override
  protected final @Nullable Indent getChildIndent() {
    throw new IllegalArgumentException("Formatting context must be used for this");
  }

  @Override
  public final @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
    return myContext.getChildAttributes(this, newChildIndex);
  }

  public boolean isLast() {
    if (myIsLast == null) {
      myIsLast = FormatterUtil.getNextNonWhitespaceSibling(myNode) == null;
    }
    return myIsLast;
  }

  public boolean isFirst() {
    if (myIsFirst == null) {
      myIsFirst = FormatterUtil.getPreviousNonWhitespaceSibling(myNode) == null;
    }
    return myIsFirst;
  }

  @Override
  public final boolean isIncomplete() {
    if (myIsIncomple == null) {
      myIsIncomple = myContext.isIncomplete(this);
      if (myIsIncomple == null) {
        myIsIncomple = super.isIncomplete();
      }
    }
    return myIsIncomple;
  }

  protected boolean shouldCreateSubBlockFor(ASTNode node) {
    IElementType elementType = PsiUtilCore.getElementType(node);
    return elementType != TokenType.WHITE_SPACE && !node.getText().isEmpty() &&
           !(HEREDOC_BODIES_TOKENSET.contains(elementType) && node.getTextLength() == 1);
  }
}
