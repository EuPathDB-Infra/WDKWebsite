package org.gusdb.wdk.model.xml;

import org.gusdb.wdk.model.UnitTestHelper;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerric
 */
public class XmlRecordClassTest {

  private final WdkModel wdkModel;

  public XmlRecordClassTest() throws Exception {
    this.wdkModel = UnitTestHelper.getModel();
  }

  /**
   * test reading question from the model
   */
  @Test
  public void testGetAllXmlRecordClasseSets() {
    // validate the references to the record classes
    for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
      String setName = rcSet.getName();
      assertTrue(setName.trim().length() > 0, "set name");

      XmlRecordClass[] recordClasses = rcSet.getRecordClasses();
      assertTrue(recordClasses.length > 0, "record class count");
      for (XmlRecordClass recordClass : recordClasses) {
        assertEquals(setName, recordClass.getRecordClassSet().getName(),
          "parent set");

        String name = recordClass.getName();
        assertTrue(name.trim().length() > 0, "record class name");

        assertNotNull(recordClass.getType(), "type");

        String fullName = recordClass.getFullName();
        assertTrue(fullName.startsWith(setName), "fullName starts with");
        assertTrue(fullName.endsWith(name), "fullName ends with");
      }
    }
  }

  @Test
  public void testGetXmlRecordClassSet() throws WdkModelException {
    for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
      String setName = rcSet.getName();

      XmlRecordClassSet set = wdkModel.getXmlRecordClassSet(setName);
      assertEquals(setName, set.getName());
    }
  }

  @Test
  public void testGetInvalidXmlRecordClassSet() {
    String setName = "NonexistXmlSet";
    assertThrows(WdkModelException.class,
      () -> wdkModel.getXmlRecordClassSet(setName));
  }

  /**
   * get record class
   */
  @Test
  public void testGetXmlRecordClass() throws WdkModelException {
    for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
      for (XmlRecordClass recordClass : rcSet.getRecordClasses()) {
        String name = recordClass.getName();

        XmlRecordClass rc = rcSet.getRecordClass(name);
        assertEquals(name, rc.getName(), "by name");

        String fullName = recordClass.getFullName();
        rc = (XmlRecordClass) wdkModel.resolveReference(fullName);
        assertEquals(fullName, rc.getFullName(), "by fullName");
      }
    }
  }

  @Test
  public void testGetInvalidXmlRecordClass() {
    String rcName = "NonexistXmlRecordClass";
    assertThrows(WdkModelException.class, () -> {
      for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
        rcSet.getRecordClass(rcName);
      }
    });
  }

  @Test
  public void testGetInvalidXmlRecordClassByFullName() {
    String fullName = "NonexistSet.NonexistXmlRecordClass";
    assertThrows(WdkModelException.class,
      () -> wdkModel.resolveReference(fullName));
  }

  @Test
  public void testGetXmlAttributes() throws WdkModelException {
    for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
      for (XmlRecordClass rc : rcSet.getRecordClasses()) {
        for (XmlAttributeField attribute : rc.getAttributeFields()) {
          String name = attribute.getName();
          assertTrue(name.trim().length() > 0, "name");
          assertTrue(attribute.getDisplayName().trim().length() > 0,
            "display name");

          XmlAttributeField attr = rc.getAttributeField(name);
          assertEquals(name, attr.getName(), "by name");
        }
      }
    }
  }

  @Test
  public void testGetInvalidXmlAttribute() {
    String attrName = "NonexistAttribute";
    assertThrows(WdkModelException.class, () -> {
      for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
        for (XmlRecordClass rc : rcSet.getRecordClasses()) {
          rc.getAttributeField(attrName);
        }
      }
    });
  }

  @Test
  public void testGetXmlTables() throws WdkModelException {
    for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
      for (XmlRecordClass rc : rcSet.getRecordClasses()) {
        for (XmlTableField table : rc.getTableFields()) {
          String name = table.getName();
          assertTrue(name.trim().length() > 0, "name");
          assertTrue(table.getDisplayName().trim().length() > 0,
            "display name");

          XmlTableField tbl = rc.getTableField(name);
          assertEquals(name, tbl.getName(), "by name");

          XmlAttributeField[] attributes = table.getAttributeFields();
          assertTrue(attributes.length > 0, "attribute count");
          for (XmlAttributeField attribute : attributes) {
            String attrName = attribute.getName();
            XmlAttributeField attr = table.getAttributeField(attrName);
            assertEquals(attrName, attr.getName(), "attribute");
          }
        }
      }
    }
  }

  @Test
  public void testGetInvalidXmlTable() {
    String attrName = "NonexistTable";
    assertThrows(WdkModelException.class, () -> {
      for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
        for (XmlRecordClass rc : rcSet.getRecordClasses()) {
          rc.getTableField(attrName);
        }
      }
    });
  }

  @Test
  public void testGetInvalidXmlAttributeByTable() {
    String attrName = "NonexistTable";
    assertThrows(WdkModelException.class, () -> {
      for (XmlRecordClassSet rcSet : wdkModel.getXmlRecordClassSets()) {
        for (XmlRecordClass rc : rcSet.getRecordClasses()) {
          for (XmlTableField table : rc.getTableFields()) {
            table.getAttributeField(attrName);
          }
        }
      }
    });
  }
}
