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

package com.perl5.lang.perl.idea.completion.util;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.perl5.PerlIcons;
import com.perl5.lang.perl.fileTypes.PerlFileTypePackage;
import com.perl5.lang.perl.idea.completion.providers.processors.PerlCompletionProcessor;
import com.perl5.lang.perl.idea.completion.providers.processors.PerlSimpleDelegatingCompletionProcessor;
import com.perl5.lang.perl.internals.PerlFeaturesTable;
import com.perl5.lang.perl.internals.PerlVersion;
import com.perl5.lang.perl.psi.PerlNamespaceDefinitionElement;
import com.perl5.lang.perl.psi.impl.PerlFileImpl;
import com.perl5.lang.perl.psi.references.PerlBuiltInNamespacesService;
import com.perl5.lang.perl.util.PerlPackageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;

import static com.perl5.PerlIcons.PACKAGE_GUTTER_ICON;
import static com.perl5.lang.perl.util.PerlPackageUtil.__PACKAGE__;


public class PerlPackageCompletionUtil {
  public static final InsertHandler<LookupElement> COMPLETION_REOPENER = (context, item) -> {
    if (context.getCompletionChar() != Lookup.AUTO_INSERT_SELECT_CHAR) {
      AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
    }
  };

  public static boolean processNamespaceLookupElement(@NotNull PerlNamespaceDefinitionElement namespaceDefinition,
                                                      @NotNull PerlCompletionProcessor completionProcessor) {
    return processPackageLookupElement(
      namespaceDefinition, ObjectUtils.notNull(namespaceDefinition.getNamespaceName(), "unnamed"), namespaceDefinition.getIcon(0),
      completionProcessor);
  }

  /**
   * @return true iff we should process
   */
  public static boolean processPackageLookupElement(@Nullable Object lookupObject,
                                                    @NotNull String packageName,
                                                    @Nullable Icon icon,
                                                    @NotNull PerlCompletionProcessor completionProcessor) {
    if (!completionProcessor.matches(packageName)) {
      return completionProcessor.result();
    }
    LookupElementBuilder result = LookupElementBuilder.create(ObjectUtils.notNull(lookupObject, packageName), packageName);

    if (PerlPackageUtil.isBuiltIn(packageName)) {
      result = result.withBoldness(true);
    }

    if (PerlPackageUtil.isPragma(packageName)) {
      result = result.withIcon(PerlIcons.PRAGMA_GUTTER_ICON);
    }
    else {
      result = result.withIcon(icon == null ? PACKAGE_GUTTER_ICON : icon);
    }

    return completionProcessor.process(result);
  }

  /**
   * @return package lookup element with automatic re-opening auto-compeltion
   */
  public static boolean processPackageLookupElementWithAutocomplete(@Nullable PerlNamespaceDefinitionElement namespaceDefinitionElement,
                                                                    @NotNull String packageName,
                                                                    @Nullable Icon icon,
                                                                    @NotNull PerlCompletionProcessor completionProcessor) {
    return processPackageLookupElement(
      namespaceDefinitionElement, packageName, icon,
      new PerlSimpleDelegatingCompletionProcessor(completionProcessor) {
        @Override
        public void addElement(@NotNull LookupElementBuilder lookupElement) {
          super.addElement(lookupElement.withInsertHandler(COMPLETION_REOPENER).withTailText("..."));
        }
      });
  }

  public static void fillWithAllNamespacesNames(@NotNull PerlCompletionProcessor completionProcessor) {
    PsiElement element = completionProcessor.getLeafElement();
    final Project project = element.getProject();
    GlobalSearchScope resolveScope = element.getResolveScope();

    Processor<PerlNamespaceDefinitionElement> namespaceProcessor =
      namespace -> PerlPackageCompletionUtil.processNamespaceLookupElement(namespace, completionProcessor);

    PerlBuiltInNamespacesService.getInstance(project).processNamespaces(namespaceProcessor);
    PerlPackageCompletionUtil.processPackageLookupElement(null, __PACKAGE__, PACKAGE_GUTTER_ICON, completionProcessor);
    processFirstNamespaceForEachName(completionProcessor, project, resolveScope, namespaceProcessor);
  }

  public static boolean processAllNamespacesNamesWithAutocompletion(@NotNull PerlCompletionProcessor completionProcessor,
                                                                    boolean withStaticExpanding,
                                                                    boolean withObjectExpanding) {
    if (!withStaticExpanding && !withObjectExpanding) {
      return completionProcessor.result();
    }
    PsiElement element = completionProcessor.getLeafElement();
    final Project project = element.getProject();
    GlobalSearchScope resolveScope = element.getResolveScope();

    Processor<PerlNamespaceDefinitionElement> namespaceProcessor = namespace -> processExpandablePackageElement(
      namespace, namespace.getNamespaceName(), completionProcessor, withStaticExpanding, withObjectExpanding);

    PerlBuiltInNamespacesService.getInstance(project).processNamespaces(namespaceProcessor);
    return processFirstNamespaceForEachName(completionProcessor, project, resolveScope, namespaceProcessor);
  }

  /**
   * Iterates all namespaces in {@code project} and {@code searchScope}, and processes the first element with each name with
   * {@code namespaceProcessor}
   */
  public static boolean processFirstNamespaceForEachName(@NotNull PerlCompletionProcessor completionProcessor,
                                                         @NotNull Project project,
                                                         @NotNull GlobalSearchScope searchScope,
                                                         @NotNull Processor<PerlNamespaceDefinitionElement> namespaceProcessor) {
    for (String packageName : PerlPackageUtil.getKnownNamespaceNames(project)) {
      PerlPackageUtil.processNamespaces(packageName, project, searchScope, namespace -> {
        String name = namespace.getNamespaceName();
        if (StringUtil.isNotEmpty(name)) {
          char firstChar = name.charAt(0);
          if (firstChar == '_' || Character.isLetterOrDigit(firstChar)) {
            namespaceProcessor.process(namespace);
            return false;
          }
        }
        return false;
      });
      if (!completionProcessor.result()) {
        return false;
      }
    }
    return completionProcessor.result();
  }

  protected static boolean processExpandablePackageElement(@NotNull PerlNamespaceDefinitionElement namespaceDefinitionElement,
                                                           @Nullable String packageName,
                                                           @NotNull PerlCompletionProcessor completionProcessor,
                                                           boolean withStaticExpanding,
                                                           boolean withObjectExpanding) {
    String prefix = completionProcessor.getResultSet().getPrefixMatcher().getPrefix();
    if (withStaticExpanding) {
      String name = packageName + PerlPackageUtil.NAMESPACE_SEPARATOR;
      if (!StringUtil.equals(name, prefix)) {
        if (!PerlPackageCompletionUtil.processPackageLookupElementWithAutocomplete(
          namespaceDefinitionElement, name, namespaceDefinitionElement.getIcon(0), completionProcessor)) {
          return false;
        }
      }
    }

    if (withObjectExpanding) {
      String name = packageName + PerlPackageUtil.DEREFERENCE_OPERATOR;
      if (!StringUtil.equals(name, prefix)) {
        return PerlPackageCompletionUtil.processPackageLookupElementWithAutocomplete(
          namespaceDefinitionElement, name, namespaceDefinitionElement.getIcon(0), completionProcessor);
      }
    }
    return completionProcessor.result();
  }


  public static void fillWithAllPackageFiles(@NotNull PerlCompletionProcessor completionProcessor) {
    PerlPackageUtil.processPackageFilesForPsiElement(completionProcessor.getLeafElement(), (packageName, file) ->
      PerlPackageCompletionUtil.processPackageLookupElement(file, packageName, null, completionProcessor));
  }

  public static void fillWithVersionNumbers(@NotNull PerlCompletionProcessor completionProcessor) {
    for (Map.Entry<String, List<String>> entry : PerlFeaturesTable.AVAILABLE_FEATURES_BUNDLES.entrySet()) {
      String version = entry.getKey();
      if (StringUtil.startsWith(version, "5")) {
        String lookupString = "v" + version;
        if (completionProcessor.matches(lookupString)) {
          completionProcessor.process(
            LookupElementBuilder.create(new PerlVersion(lookupString), lookupString).withTypeText(StringUtil.join(entry.getValue(), " "))
          );
        }
      }
    }
  }

  public static void fillWithNamespaceNameSuggestions(@NotNull PerlCompletionProcessor completionProcessor) {
    PsiFile originalFile = completionProcessor.getOriginalFile();

    VirtualFile virtualFile = originalFile.getViewProvider().getVirtualFile();
    if (virtualFile.getFileType() != PerlFileTypePackage.INSTANCE) {
      return;
    }
    completionProcessor.processSingle(LookupElementBuilder.create(virtualFile, virtualFile.getNameWithoutExtension()));
    if (!(originalFile instanceof PerlFileImpl)) {
      return;
    }
    String packageName = ((PerlFileImpl)originalFile).getFilePackageName();
    if (packageName != null) {
      completionProcessor.processSingle(LookupElementBuilder.create(originalFile, packageName));
    }
  }
}
