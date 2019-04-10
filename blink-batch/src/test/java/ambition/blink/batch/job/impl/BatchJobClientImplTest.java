package ambition.blink.batch.job.impl;

import ambition.blink.sql.SqlService;
import ambition.blink.sql.impl.SqlServiceImpl;
import java.util.List;
import java.util.Map;
import org.apache.flink.api.common.Plan;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ambition.blink.common.job.JobParameter;
import ambition.blink.job.JobClient;
import ambition.blink.sql.SqlContent;

/**
 * @Author: wpl
 */
public class BatchJobClientImplTest {

  private JobParameter jobParameter;
  private JobClient jobClient = new BatchJobClientImpl();

  @Before
  public void init() throws Exception {
    System.out.println(SqlContent.sqls);
    jobParameter = new JobParameter();
    SqlService sqlService = new SqlServiceImpl();
    Map<String, List<String>> map = sqlService.sqlConvert(SqlContent.sqls);
    jobParameter.setSqls(map);
    jobParameter.setJobName("batch_test");
  }

  @Test
  public void getJobGraphTest() throws Exception {
    JobGraph jobGraph = jobClient.getJobGraph(jobParameter, null);
    Assert.assertNotNull(jobGraph);
  }

  @Test
  public void getJobPlanTest() throws Exception {
    Plan jobPlan = jobClient.getJobPlan(jobParameter, null);
    Assert.assertNotNull(jobPlan);
  }

}
