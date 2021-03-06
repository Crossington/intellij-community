// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.intellij.openapi.keymap.KeymapUtil.getPreferredShortcutText;
import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;
import static com.intellij.psi.util.PsiTreeUtil.getStubOrPsiParentOfType;
import static com.intellij.psi.util.PsiUtilCore.getVirtualFile;
import static com.intellij.ui.ColorUtil.toHex;

public final class GutterTooltipHelper {
  private static final JBColor CONTEXT_HELP_FOREGROUND
    = JBColor.namedColor("GutterTooltip.ContextHelp.foreground", new JBColor(0x787878, 0x878787));

  private GutterTooltipHelper() {
  }

  /**
   * @param elements        a collection of elements to create a formatted tooltip text
   * @param prefix          a text to insert before all elements
   * @param skipFirstMember {@code true} to skip a method (or field) name in the link to element
   * @param actionId        an action identifier to generate context help or {@code null} if not applicable
   */
  @NotNull
  public static <E extends PsiElement> String getTooltipText(@NotNull Collection<E> elements,
                                                             @NotNull String prefix,
                                                             boolean skipFirstMember,
                                                             @Nullable String actionId) {
    String elementPrefix = 1 < elements.size() ? "<br>&nbsp;&nbsp;&nbsp;&nbsp; " : " ";
    return getTooltipText(prefix, elements, e -> elementPrefix, e -> skipFirstMember, actionId);
  }

  /**
   * @param elements  a collection of elements to create a formatted tooltip text
   * @param function  a function that returns a text to insert before the current element
   * @param predicate a function that returns {@code true} to skip a method (or field) name for the current element
   * @param actionId  an action identifier to generate context help or {@code null} if not applicable
   */
  @NotNull
  public static <E extends PsiElement> String getTooltipText(@NotNull Collection<E> elements,
                                                             @NotNull Function<E, String> function,
                                                             @NotNull Predicate<E> predicate,
                                                             @Nullable String actionId) {
    return getTooltipText(null, elements, function, predicate, actionId);
  }

  @NotNull
  private static <E extends PsiElement> String getTooltipText(@Nullable String prefix,
                                                              @NotNull Collection<E> elements,
                                                              @NotNull Function<E, String> function,
                                                              @NotNull Predicate<E> predicate,
                                                              @Nullable String actionId) {
    StringBuilder sb = new StringBuilder("<html><body>");
    if (prefix != null) sb.append(prefix);
    for (E element : elements) {
      String elementPrefix = function.apply(element);
      if (elementPrefix != null) sb.append(elementPrefix);
      appendElement(sb, element, predicate.test(element));
    }
    appendContextHelp(sb, actionId);
    sb.append("</body></html>");
    return sb.toString();
  }

  private static void appendElement(@NotNull StringBuilder sb, @NotNull PsiElement element, boolean skip) {
    boolean useSingleLink = Registry.is("gutter.tooltip.single.link");
    String packageName = null;
    boolean addedSingleLink = useSingleLink && appendLink(sb, element);
    PsiElement skipped = null;
    if (skip && (element instanceof PsiMethod || element instanceof PsiField)) {
      skipped = element; // use skipped member as first separate link
      element = getContainingElement(element);
    }
    while (element != null) {
      String name = getPresentableName(element);
      if (name != null) {
        boolean addedLink = !useSingleLink && appendLink(sb, skipped != null ? skipped : element);
        // Swing uses simple HTML processing and paints a link incorrectly if it contains different fonts.
        // This is the reason why I use monospaced font not only for element name, but for a whole link.
        // By the same reason I have to comment out support for deprecated elements.
        //
        // boolean deprecated = RefJavaUtil.isDeprecated(element);
        // if (deprecated) sb.append("<strike>");
        // sb.append("<code>");
        sb.append(name);
        // sb.append("</code>");
        // if (deprecated) sb.append("</strike>");
        if (addedLink) sb.append("</code></a>");
      }
      if (element instanceof PsiFile) break;
      PsiElement parent = getContainingElement(element);
      if (parent == null || parent instanceof PsiFile) {
        if (element instanceof PsiClass && !(element instanceof PsiAnonymousClass)) {
          String qualifiedName = ((PsiClass)element).getQualifiedName();
          if (qualifiedName != null) packageName = StringUtil.getPackageName(qualifiedName);
          break;
        }
      }
      if (parent != null) sb.append(" in ");
      element = parent;
      skipped = null;
    }
    if (addedSingleLink) sb.append("</code></a>");
    appendPackageName(sb, packageName);
  }

  private static void appendPackageName(@NotNull StringBuilder sb, @Nullable String name) {
    if (StringUtil.isEmpty(name)) return; // no package name
    sb.append(" <font color=").append(toHex(CONTEXT_HELP_FOREGROUND));
    sb.append("><code>(").append(name).append(")</code></font>");
  }

  private static void appendContextHelp(@NotNull StringBuilder sb, @Nullable String actionId) {
    if (actionId == null) return; // action id is not set
    AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action == null) return; // action is not exist
    String text = getPreferredShortcutText(action.getShortcutSet().getShortcuts());
    if (StringUtil.isEmpty(text)) return; // action have no shortcuts
    sb.append("<br><div style='margin-top: 5px'><font size='2' color='");
    sb.append(toHex(CONTEXT_HELP_FOREGROUND));
    sb.append("'>Press ").append(text).append(" to navigate</font></div>");
  }

  private static boolean appendLink(@NotNull StringBuilder sb, @NotNull PsiElement element) {
    try {
      String name = getQualifiedName(element);
      if (!StringUtil.isEmpty(name)) {
        sb.append("<a href=\"#element/").append(name).append("\"><code>");
        return true;
      }
      VirtualFile file = getVirtualFile(element);
      if (file == null) return false;

      int offset = element.getTextOffset();
      sb.append("<a href=\"#navigation/");
      sb.append(toSystemIndependentName(file.getPath()));
      sb.append(":").append(offset).append("\"><code>");
      return true;
    }
    catch (Exception ignored) {
      return false;
    }
  }

  @Nullable
  private static String getQualifiedName(@NotNull PsiElement element) {
    PsiClass psiClass = element instanceof PsiClass ? (PsiClass)element : getStubOrPsiParentOfType(element, PsiClass.class);
    if (psiClass instanceof PsiAnonymousClass) return null;
    for (QualifiedNameProvider provider : QualifiedNameProvider.EP_NAME.getExtensionList()) {
      String name = provider.getQualifiedName(element);
      if (name != null) return name;
    }
    return null;
  }

  @Nullable
  private static PsiElement getContainingElement(@NotNull PsiElement element) {
    PsiMember member = getStubOrPsiParentOfType(element, PsiMember.class);
    return member != null ? member : element.getContainingFile();
  }

  @Nullable
  private static String getPresentableName(@NotNull PsiElement element) {
    if (element instanceof PsiEnumConstantInitializer) {
      PsiEnumConstantInitializer initializer = (PsiEnumConstantInitializer)element;
      return initializer.getEnumConstant().getName();
    }
    if (element instanceof PsiAnonymousClass) {
      return "Anonymous";
    }
    if (element instanceof PsiNamedElement) {
      PsiNamedElement named = (PsiNamedElement)element;
      return named.getName();
    }
    return null;
  }
}
