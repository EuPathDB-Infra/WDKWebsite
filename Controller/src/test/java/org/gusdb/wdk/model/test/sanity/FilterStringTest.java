package org.gusdb.wdk.model.test.sanity;

import org.gusdb.fgputil.MapBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterStringTest {

  private static final Map<String, Integer[]> CASES = new MapBuilder<>(
    new LinkedHashMap<String, Integer[]>()).put("", new Integer[] {})
    .put("5", new Integer[] { 5 })
    .put("8-12", new Integer[] { 8, 9, 10, 11, 12 })
    .put("5,7,9", new Integer[] { 5, 7, 9 })
    .put("1,3,5-6", new Integer[] { 1, 3, 5, 6 })
    .put("12-15,47,129-131,206",
      new Integer[] { 12, 13, 14, 15, 47, 129, 130, 131, 206 })
    .put("1,4-7,10,12", new Integer[] { 1, 4, 5, 6, 7, 10, 12 })
    .put("3-6,9,13-15", new Integer[] { 3, 4, 5, 6, 9, 13, 14, 15 })
    .put("2-4,6-8,10-12", new Integer[] { 2, 3, 4, 6, 7, 8, 10, 11, 12 })
    .toMap();

  @Test
  public void filterStringTest() {
    for (Entry<String, Integer[]> testCase : CASES.entrySet()) {
      List<Integer> inputList = Arrays.asList(testCase.getValue());
      assertEquals(testCase.getKey(), TestFilter.getFilterString(inputList));
    }
  }
}
