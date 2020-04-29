package org.gusdb.wdk.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author xingao
 */
public class UtilityTest {

  @Test
  public void testToArray() {
    var random = UnitTestHelper.getRandom();
    var expected = new String[] { Integer.toString(random.nextInt()),
      Integer.toString(random.nextInt()), Integer.toString(random.nextInt()) };

    // comma separated list
    var input = expected[0] + "," + expected[1] + "," + expected[2];
    var actual = Utilities.toArray(input);
    assertArrayEquals(expected, actual);

    // space separated list
    input = expected[0] + " " + expected[1] + " " + expected[2];
    actual = Utilities.toArray(input);
    assertArrayEquals(expected, actual);

    // tab separated list
    input = expected[0] + "\t" + expected[1] + "\t" + expected[2];
    actual = Utilities.toArray(input);
    assertArrayEquals(expected, actual);

    // new line list
    var newline = System.getProperty("line.separator");
    input = expected[0] + newline + expected[1] + newline + expected[2];
    actual = Utilities.toArray(input);
    assertArrayEquals(expected, actual);

    // comma-space separated list
    input = expected[0] + ", " + expected[1] + ", " + expected[2];
    actual = Utilities.toArray(input);
    assertArrayEquals(expected, actual);
  }

  @Test
  public void testFromArray() {
    var random = UnitTestHelper.getRandom();
    var expected = new String[] { Integer.toString(random.nextInt()),
      Integer.toString(random.nextInt()), Integer.toString(random.nextInt()),
      Integer.toString(random.nextInt()) };

    var value = Utilities.fromArray(expected);
    var actual = Utilities.toArray(value);
    assertArrayEquals(expected, actual);
  }
}
