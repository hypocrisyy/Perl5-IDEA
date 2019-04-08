/*
 * Copyright 2015-2018 Alexandr Evstigneev
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

package com.perl5.lang.perl.idea.codeInsight.typeInference.value;

import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PerlSpecialValue extends PerlValue {

  protected PerlSpecialValue() {
  }

  private PerlSpecialValue(@Nullable PerlValue bless) {
  }

  private PerlSpecialValue(@NotNull StubInputStream dataStream) {
  }

  @Override
  protected void serializeData(@NotNull StubOutputStream dataStream) {
  }

  @Override
  public final boolean equals(Object o) {
    return o == this;
  }

  @Override
  protected final int computeHashCode() {
    return getClass().hashCode();
  }

  @NotNull
  @Override
  final PerlValue createBlessedCopy(@NotNull PerlValue bless) {
    return this;
  }
}