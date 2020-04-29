package org.gusdb.wdk.model.query;

import org.gusdb.fgputil.TestUtil;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.dbms.ResultList;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({ "SqlNoDataSourceInspection", "SqlResolve" })
public class ResultListArgBatchTest {

  private static final String TABLE_SQL = "CREATE TABLE value_table ( "
    + "  id INTEGER NOT NULL, "
    + "  value INTEGER, "
    + ")";

  private static final String INSERT_SQL = "INSERT INTO value_table VALUES (?, ?)";
  private static final String COUNT_SQL = "SELECT count(1) FROM value_table";
  private static final String DROP_TABLE_SQL = "DROP TABLE value_table";

  private static final String[][] COLUMN_CONFIG = new String[][] {
    { "NUMBER", "ID" }, { "NUMBER", "VALUE" } };

  private static class SimpleResultList implements ResultList {

    private final int _numRecords;
    private int _currentRowIndex = 0;

    public SimpleResultList(int numRecords) {
      assertTrue(numRecords >= 0);
      _numRecords = numRecords;
    }

    @Override
    public boolean next() {
      _currentRowIndex++;
      return (_currentRowIndex <= _numRecords);
    }

    @Override
    public Object get(String columnName) {
      return "1";
    }

    @Override
    public boolean contains(String columnName) {
      return true;
    }

    @Override
    public void close() {
      // don't need to do anything here
    }

  }

  @Test
  public void testResultListArgBatch() throws Exception {
    Integer[] sampleInsertCounts = { 0, 1, 3, 5, 10, 13, 27 };
    Integer[] sampleBatchSizes = { 0, 1, 2, 4, 5 };
    for (int insertCount : sampleInsertCounts) {
      for (int batchSize : sampleBatchSizes) {
        System.out.println("Running test with batchSize = "
          + batchSize
          + ", insertCount = "
          + insertCount);
        runTest(insertCount, batchSize);
      }
    }
  }

  private void runTest(final int insertCount, int batchSize) throws Exception {
    DataSource ds = TestUtil.getTestDataSource("batchDb");
    new SQLRunner(ds, TABLE_SQL).executeStatement();
    ResultList resultList = new SimpleResultList(insertCount);
    List<Column> cols = getColumns();
    ResultListArgumentBatch argBatch = new ResultListArgumentBatch(resultList,
      cols, batchSize);
    new SQLRunner(ds, INSERT_SQL).executeStatementBatch(argBatch);
    new SQLRunner(ds, COUNT_SQL).executeQuery(rs -> {
      if (rs.next()) {
        assertEquals(insertCount, rs.getInt(1));
      }
      else {
        // have no idea why this returns no rows if no inserts are none
        assertEquals(insertCount, 0);
        //throw new RuntimeException("Should have received a count.");
      }
      return this;
    });
    new SQLRunner(ds, DROP_TABLE_SQL).executeStatement();
  }

  private static List<Column> getColumns() throws WdkModelException {
    List<Column> list = new ArrayList<>();
    for (String[] colConfig : COLUMN_CONFIG) {
      Column col = new Column();
      col.setColumnType(colConfig[0]);
      col.setName(colConfig[1]);
      list.add(col);
    }
    return list;
  }

}
