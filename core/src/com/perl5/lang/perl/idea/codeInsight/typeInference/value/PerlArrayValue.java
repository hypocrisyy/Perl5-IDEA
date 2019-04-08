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
import com.perl5.PerlBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class PerlArrayValue extends PerlListValue {

  private PerlArrayValue(@NotNull List<PerlValue> elements, @Nullable PerlValue bless) {
    super(elements, bless);
  }

  PerlArrayValue(@NotNull StubInputStream dataStream) throws IOException {
    super(dataStream);
  }

  @Override
  protected int getSerializationId() {
    return PerlValuesManager.ARRAY_ID;
  }

  @NotNull
  @Override
  PerlValue createBlessedCopy(@NotNull PerlValue bless) {
    return new PerlArrayValue(getElements(), bless);
  }

  @Override
  public int computeHashCode() {
    return super.computeHashCode() * 31 + getClass().hashCode();
  }


  @Override
  public String toString() {
    return "Array: " + getElements().toString();
  }

  @NotNull
  @Override
  public String getPresentableValueText() {
    return PerlBundle.message("perl.value.array.presentable",
                              getElements().stream().map(PerlValue::getPresentableText).collect(Collectors.joining(", ")));
  }
}