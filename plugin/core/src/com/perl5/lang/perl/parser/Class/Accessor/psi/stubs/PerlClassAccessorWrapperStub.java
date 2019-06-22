/*
 * Copyright 2015-2019 Alexandr Evstigneev
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

package com.perl5.lang.perl.parser.Class.Accessor.psi.stubs;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.perl5.lang.perl.parser.Class.Accessor.psi.impl.PerlClassAccessorWrapper;
import com.perl5.lang.perl.psi.stubs.PerlPolyNamedElementStub;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PerlClassAccessorWrapperStub extends PerlPolyNamedElementStub<PerlClassAccessorWrapper> {
  private final boolean myIsFollowBestPractice;

  public PerlClassAccessorWrapperStub(StubElement parent,
                                      IStubElementType elementType,
                                      @NotNull List<StubElement> lightNamedElementsStubs,
                                      boolean isFollowBestPractice) {
    super(parent, elementType, lightNamedElementsStubs);
    myIsFollowBestPractice = isFollowBestPractice;
  }

  public boolean isFollowBestPractice() {
    return myIsFollowBestPractice;
  }

  @Override
  public String toString() {
    return super.toString() + "\n" +
           "\tFollow best practice: " + myIsFollowBestPractice;
  }
}
