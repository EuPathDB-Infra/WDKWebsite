package org.gusdb.wdk.model.query;

import org.gusdb.wdk.model.UnitTestHelper;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.param.ParamValuesSet;
import org.gusdb.wdk.model.query.spec.QueryInstanceSpec;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordClassSet;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.test.ParamValuesFactory;
import org.gusdb.wdk.model.user.StepContainer;
import org.gusdb.wdk.model.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author xingao
 */
public class TableQueryTest {

  private static final boolean ASSERTION = false;

  private final User user;
  private final WdkModel wdkModel;

  public TableQueryTest() throws Exception {
    this.user = UnitTestHelper.getRegisteredUser();
    this.wdkModel = UnitTestHelper.getModel();
  }

  @Test
  public void testTableQueries() throws WdkModelException {
    for (RecordClassSet recordClassSet : wdkModel.getAllRecordClassSets()) {
      for (RecordClass recordClass : recordClassSet.getRecordClasses()) {
        for (TableField table : recordClass.getTableFields()) {
          Query query = table.getWrappedQuery();
          if (query.getDoNotTest())
            continue;
          for (ParamValuesSet valueSet : ParamValuesFactory.getParamValuesSets(
            user, query)) {
            Map<String, String> values = valueSet.getParamValues();
            if (values.size() == 0)
              continue;
            int min = valueSet.getMinRows();
            int max = valueSet.getMaxRows();
            QueryInstance<?> instance = Query.makeQueryInstance(
              QueryInstanceSpec.builder()
                .putAll(values)
                .buildRunnable(user, query, StepContainer.emptyContainer()));
            int result = instance.getResultSize();
            if (ASSERTION) {
              Assertions.assertTrue(result >= min, result + " >= " + min);
              Assertions.assertTrue(result <= max, result + " <= " + max);
            }
          }
        }

      }
    }
  }
}
