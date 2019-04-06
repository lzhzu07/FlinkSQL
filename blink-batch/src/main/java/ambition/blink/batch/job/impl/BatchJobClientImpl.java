package ambition.blink.batch.job.impl;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.Plan;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.table.api.BatchQueryConfig;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.java.BatchTableEnvironment;
import org.apache.flink.table.sinks.BatchTableSink;
import org.apache.flink.table.sources.BatchTableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ambition.blink.batch.BatchLocalEnvironment;
import ambition.blink.batch.BatchTableUtils;
import ambition.blink.common.job.JobParameter;
import ambition.blink.common.table.TableInfo;
import ambition.blink.common.table.ViewOrInsertInfo;
import ambition.blink.job.JobClient;
import ambition.blink.batch.sql.SqlConstant;
import ambition.blink.batch.sql.SqlService;
import ambition.blink.batch.sql.impl.SqlServiceImpl;

/**
 * @Author: wpl
 */
public class BatchJobClientImpl implements JobClient {
  private static final Logger LOG = LoggerFactory.getLogger(BatchJobClientImpl.class);

  private SqlService sqlService = new SqlServiceImpl();

  @Override
  public JobGraph getJobGraph(JobParameter jobParam, Map<String,String> extParams) throws Exception {
    BatchLocalEnvironment env = getBatchLocalEnvironmentInfo(jobParam, extParams);
    return env.getJobGraph(jobParam.getJobName());
  }

  @Override
  public Plan getJobPlan(JobParameter jobParam, Map<String,String> extParams) throws Exception {
    BatchLocalEnvironment env = getBatchLocalEnvironmentInfo(jobParam, extParams);
    return env.getJobPlan(jobParam.getJobName());
  }

  @Override
  public StreamGraph getStreamGraph(JobParameter jobParameter, Map<String, String> extParams)
      throws Exception {
    return null;
  }

  private BatchLocalEnvironment getBatchLocalEnvironmentInfo(JobParameter jobParam, Map<String, String> extParams) throws Exception {
    BatchLocalEnvironment env = new BatchLocalEnvironment();
    BatchTableEnvironment tEnv = TableEnvironment.getTableEnvironment(env);

    // sqls
    Map<String, List<String>> sqls = jobParam.getSqls();

    //udf

    // config
    BatchQueryConfig queryConfig = tEnv.queryConfig();

    // source
    List<String> sourceSqls = sqls.get(SqlConstant.TABLE_SOURCE);
    if (CollectionUtils.isEmpty(sourceSqls)) {
      throw new IllegalArgumentException("source table is not null");
    }

    //sink
    List<String> sinkSqls = sqls.get(SqlConstant.TABLE_SINK);
    if (CollectionUtils.isEmpty(sinkSqls)) {
      throw new IllegalArgumentException("sink table is not null");
    }

    for (String sql: sourceSqls) {
      TableInfo tableInfo = sqlService.sqlTableParser(sql);

      BatchTableSource tableSource = BatchTableUtils.getBatchTableSource(tableInfo, extParams);

      tEnv.registerTableSource(tableInfo.getTableName(), tableSource);
    }

    for (String sql: sinkSqls) {
      TableInfo tableInfo = sqlService.sqlTableParser(sql);
      Map<String, TypeInformation<?>> outputSchemaMap = tableInfo.getFlinkUseSchema();
      String[] fieldNames = new String[outputSchemaMap.size()];
      String[] fieldNamesArray = outputSchemaMap.keySet().toArray(fieldNames);

      TypeInformation[] fieldTypes = new TypeInformation[outputSchemaMap.size()];
      TypeInformation[] fieldTypesArray = outputSchemaMap.values().toArray(fieldTypes);

      BatchTableSink tableSink = BatchTableUtils.getBatchTableSink(tableInfo, extParams);
      // 查看该方法的源码使用
      tEnv.registerTableSink(tableInfo.getTableName(), fieldNamesArray, fieldTypesArray, tableSink);
    }

    //是否含有view的操作
    if (sqls.containsKey(SqlConstant.TABLE_VIEW)){
      //视图sql
      List<String> viewSqls = sqls.get(SqlConstant.TABLE_VIEW);
      if (CollectionUtils.isNotEmpty(viewSqls)){
        for (String sql : viewSqls){
          ViewOrInsertInfo viewInfo = sqlService.sqlViewParser(sql);

          String tableName = viewInfo.getTableName();
          String selectBody = viewInfo.getExecSql();

          Table viewTable = tEnv.sqlQuery(selectBody);
          tEnv.registerTable(tableName, viewTable);
          LOG.info("sql query info {}", selectBody);
        }
      }
    }

    List<String> dmlSqls = sqls.get(SqlConstant.INSERT_INTO);
    if (CollectionUtils.isNotEmpty(dmlSqls)) {
      for (String sql: dmlSqls){
        ViewOrInsertInfo insertInfo = sqlService.sqlDmlParser(sql);
        String selectBody = insertInfo.getExecSql();
        tEnv.sqlUpdate(selectBody,queryConfig);
        LOG.info("sql update info {}", selectBody);
      }
    }
    return env;
  }

}
