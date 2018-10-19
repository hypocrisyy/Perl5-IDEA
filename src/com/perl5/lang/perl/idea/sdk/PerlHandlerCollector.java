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

package com.perl5.lang.perl.idea.sdk;

import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.KeyedExtensionCollector;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PerlHandlerCollector<Handler extends AbstractPerlHandler<?, ?>> extends KeyedExtensionCollector<Handler, String> {
  @NotNull
  private final String myEpName;

  private final AtomicNotNullLazyValue<List<Handler>> myHandlers = AtomicNotNullLazyValue.createValue(
    () -> ContainerUtil.map(getPoint().getExtensions(), KeyedLazyInstance::getInstance));

  public PerlHandlerCollector(@NotNull String epName) {
    super(epName);
    myEpName = epName;
  }

  public List<Handler> getExtensions() {
    return myHandlers.getValue();
  }

  @NotNull
  private ExtensionPoint<KeyedLazyInstance<Handler>> getPoint() {
    return Extensions.getRootArea().getExtensionPoint(myEpName);
  }
}
