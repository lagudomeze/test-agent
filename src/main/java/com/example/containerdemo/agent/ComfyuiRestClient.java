package com.example.containerdemo.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

@HttpExchange
public interface ComfyuiRestClient {

    /// @param promptId 提交的任务id
    /// @param others 其他字段
    record SubmitResult(@JsonAlias("prompt_id") String promptId,
                        @JsonAnySetter Map<String, JsonNode> others) {
    }

    @PostExchange("/prompt")
    SubmitResult submitTask(@RequestBody /* todo  replace real type */ Object task);

    /// @param running 运行中的任务id列表
    /// @param pending 等待中的任务id列表
    record QueueStatus(
            @JsonAlias("queue_running")
            List<String> running,
            @JsonAlias("queue_pending")
            List<String> pending) {
    }

    @GetExchange("/queue")
    QueueStatus queueStatus();

    /// from [v0.3.45/server.py#L706](https://github.com/comfyanonymous/ComfyUI/blob/v0.3.45/server.py#L706) 只清理排队中的任务
    ///  @param clear 直接清空队列
    /// @param delete 指定要删除的任务id列表
    record CleanPendingTask(boolean clear, List<String> delete) {
    }

    /// 
    /// @param request 清理请求
    @PostExchange("/queue")
    void queue(@RequestBody CleanPendingTask request);

    /// 尝试打断执行中的任务
    @PostExchange("/interrupt")
    void interrupt();
}
