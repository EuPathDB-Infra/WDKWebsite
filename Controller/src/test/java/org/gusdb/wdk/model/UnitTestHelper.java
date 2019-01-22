/**
 * 
 */
package org.gusdb.wdk.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.dbms.CacheFactory;
import org.gusdb.wdk.model.query.SqlQuery;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.DatasetParam;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.query.param.ParamValuesSet;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.question.QuestionSet;
import org.gusdb.wdk.model.test.ParamValuesFactory;
import org.gusdb.wdk.model.user.UnregisteredUser.UnregisteredUserType;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.StepFactory;
import org.gusdb.wdk.model.user.StepUtilities;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.model.user.UserFactory;

/**
 * @author xingao
 * 
 */
public class UnitTestHelper {

    public static final String REGISTERED_USER_EMAIL = "wdktest@gusdb.org";
    public static final String REGISTERED_USER_PASSWORD = "qK3W5vF?=y";

    private static final Logger logger = Logger.getLogger(UnitTestHelper.class);

    // use a fixed random number generator in order to use cache.
    private static Random random = new Random(1);

    private static WdkModel _wdkModel;
    private static User guest;
    private static User registeredUser;

    private static List<Question> normalQuestions;
    private static List<Question> datasetQuestions;

    public static Random getRandom() {
        return random;
    }

    public synchronized static WdkModel getModel() throws WdkModelException {
        if (_wdkModel == null) {
            logger.info("Loading model...");
            String projectId = System.getProperty(Utilities.ARGUMENT_PROJECT_ID);
            String gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);
            _wdkModel = WdkModel.construct(projectId, gusHome);

            // reset the cache
            logger.info("resetting cache...");
            CacheFactory cacheFactory = _wdkModel.getResultFactory().getCacheFactory();
            cacheFactory.resetCache(true, true);
        }
        return _wdkModel;
    }

    public synchronized static User getGuest() throws WdkModelException {
        if (guest == null) {
            guest = getModel().getUserFactory().createUnregistedUser(UnregisteredUserType.GUEST);
        }
        StepFactory stepFactory = getModel().getStepFactory();
        stepFactory.deleteStrategies(guest, false);
        stepFactory.deleteSteps(guest, false);
        return guest;
    }

    public synchronized static User getRegisteredUser() throws WdkModelException, WdkUserException {
        if (registeredUser == null) {
            WdkModel wdkModel = getModel();
            UserFactory userFactory = wdkModel.getUserFactory();
            // check if user exist
            registeredUser = userFactory.getUserByEmail(REGISTERED_USER_EMAIL);
            if (registeredUser == null) {
                // user doesn't exist, create one
                registeredUser = userFactory.createUser(REGISTERED_USER_EMAIL, null, null, null);
                userFactory.changePassword(registeredUser.getUserId(), REGISTERED_USER_PASSWORD);
            }
        }
        // registeredUser.deleteStrategies();
        // registeredUser.deleteSteps();
        return registeredUser;
    }

    /**
     * Randomly pick a normal question. A normal question is one without
     * answerParam nor datasetParam
     * 
     * @return
     */
    public static Question getNormalQuestion() throws WdkModelException {
        if (normalQuestions == null) loadQuestions();
        return normalQuestions.get(random.nextInt(normalQuestions.size()));
    }

    public static Question getDatasetParamQuestion() throws WdkModelException {
        if (datasetQuestions == null) loadQuestions();
        return datasetQuestions.get(random.nextInt(datasetQuestions.size()));
    }

    public static Step createNormalStep(User user) throws WdkModelException {
        Question question = getNormalQuestion();
        List<ParamValuesSet> paramValueSets = ParamValuesFactory.getParamValuesSets(user, question.getQuery());
        ParamValuesSet paramValueSet = paramValueSets.get(random.nextInt(paramValueSets.size()));
        Map<String, String> params = paramValueSet.getParamValues();
        return StepUtilities.createStep(user, null, question, params, (String) null, false, 0);
    }

    private static void loadQuestions() throws WdkModelException {
        WdkModel wdkModel = getModel();
        Map<String, List<Question>> allNormalQuestions = new HashMap<String, List<Question>>();
        Map<String, List<Question>> allAnswerQuestions = new HashMap<String, List<Question>>();
        Map<String, List<Question>> allDatasetQuestions = new HashMap<String, List<Question>>();

        for (QuestionSet questionSet : wdkModel.getAllQuestionSets()) {
            for (Question question : questionSet.getQuestions()) {
                String rcName = question.getRecordClass().getFullName();

                if (!(question.getQuery() instanceof SqlQuery)) continue;

                if (!allAnswerQuestions.containsKey(rcName)) {
                    allAnswerQuestions.put(rcName, new ArrayList<Question>());
                    allDatasetQuestions.put(rcName, new ArrayList<Question>());
                    allNormalQuestions.put(rcName, new ArrayList<Question>());
                }

                boolean hasAnswerParam = false;
                boolean hasDatasetParam = false;
                for (Param param : question.getParams()) {
                    if (param instanceof AnswerParam) hasAnswerParam = true;
                    if (param instanceof DatasetParam) hasDatasetParam = true;
                }
                if (hasAnswerParam)
                    allAnswerQuestions.get(rcName).add(question);
                if (hasDatasetParam)
                    allDatasetQuestions.get(rcName).add(question);
                if (!hasAnswerParam && !hasDatasetParam
                        && !question.getQuery().getDoNotTest())
                    allNormalQuestions.get(rcName).add(question);
            }
        }

        // now pick one with most questions
        String maxRcName = null;
        int max = -1;
        for (String rcName : allNormalQuestions.keySet()) {
            int count = allAnswerQuestions.get(rcName).size()
                    + allDatasetQuestions.get(rcName).size()
                    + allNormalQuestions.get(rcName).size();
            if (count > max) {
                maxRcName = rcName;
                max = count;
            }
        }
        normalQuestions = allNormalQuestions.get(maxRcName);
        datasetQuestions = allDatasetQuestions.get(maxRcName);
    }
}
