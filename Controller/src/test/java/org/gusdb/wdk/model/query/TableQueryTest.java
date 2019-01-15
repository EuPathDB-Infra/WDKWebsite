/**
 * 
 */
package org.gusdb.wdk.model.query;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.UnitTestHelper;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.param.ParamValuesSet;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordClassSet;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.test.ParamValuesFactory;
import org.gusdb.wdk.model.user.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author xingao
 * 
 */
public class TableQueryTest {

    private static final boolean ASSERTION = false;
    
    private User user;
    private WdkModel wdkModel;

    public TableQueryTest() throws Exception {
        this.user = UnitTestHelper.getRegisteredUser();
        this.wdkModel = UnitTestHelper.getModel();
    }

    @Test
    public void testTableQueries() throws WdkModelException, WdkUserException {
        for (RecordClassSet recordClassSet : wdkModel.getAllRecordClassSets()) {
            for (RecordClass recordClass : recordClassSet.getRecordClasses()) {
                for (TableField table : recordClass.getTableFields()) {
                    Query query = table.getWrappedQuery();
                    if (query.getDoNotTest())
                        continue;
                    for (ParamValuesSet valueSet : ParamValuesFactory.getParamValuesSets(user, query)) {
                        Map<String, String> values = valueSet.getParamValues();
                        if (values.size() == 0)
                            continue;
                        int min = valueSet.getMinRows();
                        int max = valueSet.getMaxRows();
                        QueryInstance<?> instance = query.makeInstance(user,
                                values, true, 0,
                                new LinkedHashMap<String, String>());
                        int result = instance.getResultSize();
                        if (ASSERTION) {
                        	Assert.assertTrue(result + " >= " + min, result >= min);
                        	Assert.assertTrue(result + " <= " + max, result <= max);
                        }
                    }
                }

            }
        }
    }
}
