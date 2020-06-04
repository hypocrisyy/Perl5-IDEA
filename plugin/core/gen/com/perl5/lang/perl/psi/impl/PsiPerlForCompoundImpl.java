// This is a generated file. Not intended for manual editing.
package com.perl5.lang.perl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.perl5.lang.perl.lexer.PerlElementTypesGenerated.*;
import com.perl5.lang.perl.psi.*;

public class PsiPerlForCompoundImpl extends PerlCompositeElementImpl implements PsiPerlForCompound {

  public PsiPerlForCompoundImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiPerlVisitor visitor) {
    visitor.visitForCompound(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPerlVisitor) accept((PsiPerlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiPerlBlock getBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPerlBlock.class);
  }

  @Override
  @Nullable
  public PsiPerlForCondition getForCondition() {
    return PsiTreeUtil.getChildOfType(this, PsiPerlForCondition.class);
  }

  @Override
  @Nullable
  public PsiPerlForInit getForInit() {
    return PsiTreeUtil.getChildOfType(this, PsiPerlForInit.class);
  }

  @Override
  @Nullable
  public PsiPerlForMutator getForMutator() {
    return PsiTreeUtil.getChildOfType(this, PsiPerlForMutator.class);
  }

}