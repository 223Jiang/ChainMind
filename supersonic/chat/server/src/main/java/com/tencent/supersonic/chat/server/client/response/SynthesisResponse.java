package com.tencent.supersonic.chat.server.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * @author JiangWeiWei
 */
@NoArgsConstructor(force = true)
@Getter
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知字段，增强 JSON 容错性
@JsonInclude(JsonInclude.Include.NON_NULL) // 忽略 null 值字段
public class SynthesisResponse {

    @JsonProperty("log_id")
    private final long logId;

    /**
     * 任务信息
     */
    @JsonProperty("tasks_info")
    private final List<TaskInfo> tasksInfo;

    public SynthesisResponse(long logId, List<TaskInfo> tasksInfo) {
        this.logId = logId;
        // 防止空指针异常
        this.tasksInfo = tasksInfo == null ? Collections.emptyList() : tasksInfo;
    }
}


@NoArgsConstructor(force = true)
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class TaskInfo {

    /**
     * 任务id
     */
    @JsonProperty("task_id")
    private final String taskId;

    /**
     * 任务状态
     */
    @JsonProperty("task_status")
    private final String taskStatus;

    /**
     * 任务结果
     */
    @JsonProperty("task_result")
    private final TaskResult taskResult;

    public TaskInfo(String taskId, String taskStatus, TaskResult taskResult) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be null or empty");
        }
        this.taskId = taskId;
        this.taskStatus = taskStatus;
        this.taskResult = taskResult;
    }
}


@NoArgsConstructor(force = true)
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class TaskResult {

    /**
     * 错误码
     */
    @JsonProperty("err_no")
    private final String errNo;

    /**
     * 错误信息
     */
    @JsonProperty("err_msg")
    private final String errMsg;

    @JsonProperty("sn")
    private final String sn;

    /**
     * 音频下载链接
     */
    @JsonProperty("speech_url")
    private final String speechUrl;

    public TaskResult(String errNo, String errMsg, String sn, String speechUrl) {
        if (errNo == null || errNo.isEmpty()) {
            throw new IllegalArgumentException("Error code cannot be null or empty");
        }
        this.errNo = errNo;
        this.errMsg = errMsg;
        this.sn = sn;
        this.speechUrl = speechUrl;
    }
}
