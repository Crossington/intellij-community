package com.jetbrains.python.documentation;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vlan
 */

/**
 * Substring with explicit offsets within its parent string.
 * <p>
 * Regular java.lang.String objects share a single char buffer for results of substring(), trim(), etc., but the offset and count
 * fields of Strings are unfortunately private.
 */
public class Substring implements CharSequence {
  private static final Pattern RE_NL = Pattern.compile("(\\r?\\n)");

  private final String myString;
  private final int myStartOffset;
  private final int myEndOffset;

  public Substring(String s) {
    this(s, 0, s.length());
  }

  public Substring(String s, int start, int end) {
    myString = s;
    myStartOffset = start;
    myEndOffset = end;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof String) {
      return toString().equals(o);
    }
    else if (o instanceof Substring) {
      return toString().equals(o.toString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return getTextRange().substring(myString);
  }

  public String getSuperString() {
    return myString;
  }

  public TextRange getTextRange() {
    return TextRange.create(myStartOffset, myEndOffset);
  }

  public List<Substring> split(String regex) {
    return split(Pattern.compile(regex));
  }

  public List<Substring> split(Pattern pattern) {
    final List<Substring> result = new ArrayList<Substring>();
    final Matcher m = pattern.matcher(myString);
    int start = myStartOffset;
    int end = myEndOffset;
    if (m.find(start)) {
      do {
        end = m.start();
        result.add(createAnotherSubstring(start, Math.min(end, myEndOffset)));
        start = m.end();
      } while (end < myEndOffset && m.find());
      if (start < myEndOffset) {
        result.add(createAnotherSubstring(start, myEndOffset));
      }
    } else {
      result.add(createAnotherSubstring(start, end));
    }
    return result;
  }

  public List<Substring> splitLines() {
    return split(RE_NL);
  }

  public Substring trim() {
    int start;
    int end;
    for (start = myStartOffset; start < myEndOffset && myString.charAt(start) <= '\u0020'; start++) {
    }
    for (end = myEndOffset - 1; end > start && myString.charAt(end) <= '\u0020'; end--) {
    }
    return createAnotherSubstring(start, end + 1);
  }

  public Substring getMatcherGroup(Matcher m, int group) {
    return substring(m.start(group), m.end(group));
  }

  @Override
  public int length() {
    return myEndOffset - myStartOffset;
  }

  @Override
  public char charAt(int i) {
    return myString.charAt(myStartOffset + i);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return substring(start,  end);
  }

  public boolean startsWith(String prefix) {
    return indexOf(prefix) == 0;
  }

  public int indexOf(String s) {
    int n = myString.indexOf(s, myStartOffset);
    return n < myEndOffset ? n - myStartOffset : -1;
  }

  @SuppressWarnings({"MethodNamesDifferingOnlyByCase"})
  public Substring substring(int start) {
    return substring(start, length());
  }

  @SuppressWarnings({"MethodNamesDifferingOnlyByCase"})
  public Substring substring(int start, int end) {
    return createAnotherSubstring(myStartOffset + start, myStartOffset + end);
  }

  public String concatTrimmedLines(String separator) {
    final StringBuilder b = new StringBuilder();
    List<Substring> lines = splitLines();
    final int n = lines.size();
    for (int i = 0; i < n; i++) {
      b.append(lines.get(i).trim().toString());
      if (i < n - 1) {
        b.append(separator);
      }
    }
    return b.toString();
  }

  private Substring createAnotherSubstring(int start, int end) {
    return new Substring(myString, start, end);
  }
}
