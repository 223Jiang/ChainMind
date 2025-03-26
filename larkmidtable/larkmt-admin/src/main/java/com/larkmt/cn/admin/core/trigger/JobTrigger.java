package com.larkmt.cn.admin.core.trigger;

import com.larkmt.cn.admin.core.thread.JobTriggerPoolHelper;
import com.larkmt.rpc.util.IpUtil;
import com.larkmt.cn.admin.core.conf.JobAdminConfig;
import com.larkmt.cn.admin.core.route.ExecutorRouteStrategyEnum;
import com.larkmt.cn.admin.core.scheduler.JobScheduler;
import com.larkmt.cn.admin.core.util.I18nUtil;
import com.larkmt.cn.admin.entity.JobDatasource;
import com.larkmt.cn.admin.entity.JobGroup;
import com.larkmt.cn.admin.entity.JobInfo;
import com.larkmt.cn.admin.entity.JobLog;
import com.larkmt.cn.admin.tool.query.BaseQueryTool;
import com.larkmt.cn.admin.tool.query.QueryToolFactory;
import com.larkmt.cn.admin.util.JSONUtils;
import com.larkmt.core.biz.ExecutorBiz;
import com.larkmt.core.biz.impl.ExecutorBizImpl;
import com.larkmt.core.biz.model.ReturnT;
import com.larkmt.core.biz.model.TriggerParam;
import com.larkmt.core.enums.ExecutorBlockStrategyEnum;
import com.larkmt.core.enums.IncrementTypeEnum;
import com.larkmt.core.glue.GlueTypeEnum;
import io.netty.util.internal.ThrowableUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

/**
 * xxl-job trigger
 * Created by xuxueli on 17/7/13.
 */
public class JobTrigger {
    private static Logger logger = LoggerFactory.getLogger(JobTrigger.class);

    /**
     * trigger job
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount        >=0: use this param
     *                              <0: use param from job info config
     * @param executorShardingParam
     * @param executorParam         null: use job param
     *                              not null: cover job param
     */
    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam) {
		JobInfo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoMapper().loadById(jobId);
		if (jobInfo == null) {
			logger.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
			return;
		}
		if (GlueTypeEnum.BEAN.getDesc().equals(jobInfo.getGlueType())) {
			//解密账密
			String json = JSONUtils.changeJson(jobInfo.getJobJson(), JSONUtils.decrypt);
			jobInfo.setJobJson(json);
		}
		if (StringUtils.isNotBlank(executorParam)) {
			jobInfo.setExecutorParam(executorParam);
		}
		int finalFailRetryCount = failRetryCount >= 0 ? failRetryCount : jobInfo.getExecutorFailRetryCount();
		JobGroup group = JobAdminConfig.getAdminConfig().getJobGroupMapper().load(jobInfo.getJobGroup());

		// sharding param
		int[] shardingParam = null;
		if (executorShardingParam != null) {
			String[] shardingArr = executorShardingParam.split("/");
			if (shardingArr.length == 2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
				shardingParam = new int[2];
				shardingParam[0] = Integer.valueOf(shardingArr[0]);
				shardingParam[1] = Integer.valueOf(shardingArr[1]);
			}
		}
		if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null)
				&& group.getRegistryList() != null && !group.getRegistryList().isEmpty()
				&& shardingParam == null) {
			for (int i = 0; i < group.getRegistryList().size(); i++) {
				processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryList().size());
			}
		} else {
			if (shardingParam == null) {
				shardingParam = new int[]{0, 1};
			}
			System.out.println("processTrigger");
			processTrigger(group, jobInfo, finalFailRetryCount, triggerType, shardingParam[0], shardingParam[1]);
		}

    }

    private static boolean isNumeric(String str) {
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param group               job group, registry list may be empty
     * @param jobInfo
     * @param finalFailRetryCount
     * @param triggerType
     * @param index               sharding index
     * @param total               sharding index
     */
    private static void processTrigger(JobGroup group, JobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total) {

        JobTriggerPoolHelper jobTriggerPoolHelper =  new JobTriggerPoolHelper();
        jobTriggerPoolHelper.runJob(jobInfo.getId());

    }

    private static long getMaxId(JobInfo jobInfo) {
        JobDatasource datasource = JobAdminConfig.getAdminConfig().getJobDatasourceMapper().selectById(jobInfo.getDatasourceId());
        BaseQueryTool qTool = QueryToolFactory.getByDbType(datasource);
        return qTool.getMaxIdVal(jobInfo.getReaderTable(), jobInfo.getPrimaryKey());
    }

    /**
     * run executor
     *
     * @param triggerParam
     * @param address
     * @return
     */
    public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address) {
        ReturnT<String> runResult = null;
        try {
            // 进行任务的触发
            ExecutorBiz executorBiz = new ExecutorBizImpl();
            executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> LarkMidTable trigger error, please check if the executor[{}] is running.", address, e);
            runResult = new ReturnT<String>(ReturnT.FAIL_CODE, ThrowableUtil.stackTraceToString(e));
        }

        StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
        runResultSB.append("<br>address：").append(address);
//        runResultSB.append("<br>code：").append(runResult.getCode());
//        runResultSB.append("<br>msg：").append(runResult.getMsg());

//        runResult.setMsg(runResultSB.toString());
        return runResult;
    }

}
