package io.github.ih0rd.adapter.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Simple converter using to transform string between cases */
public class StringCaseConverter {

  /**
   * @param snakeCase Input string in snake_case
   * @return Converted string in camelCase
   */
  public static String snakeToCamel(String snakeCase) {
    if (snakeCase == null || snakeCase.isEmpty()) {
      return snakeCase;
    }
    var parts = snakeCase.split("_");
    return parts[0]
        + Arrays.stream(parts, 1, parts.length)
            .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
            .collect(Collectors.joining());
  }

  /**
   * @param camelCase Input string in cameCase
   * @return Converted string in snake_case
   */
  public static String camelToSnake(String camelCase) {
    if (camelCase == null || camelCase.isEmpty()) {
      return camelCase;
    }
    StringBuilder result = new StringBuilder();
    result.append(
        Character.toLowerCase(
            camelCase.charAt(0))); // Convert the first character to lowercase directly

    camelCase
        .substring(1) // Skip the first character
        .chars()
        .mapToObj(
            c -> Character.isUpperCase(c) ? "_" + Character.toLowerCase((char) c) : "" + (char) c)
        .forEach(result::append); // Collect directly into the StringBuilder

    return result.toString();
  }
}
