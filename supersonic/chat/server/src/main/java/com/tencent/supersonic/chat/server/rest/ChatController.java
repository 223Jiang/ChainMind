package com.tencent.supersonic.chat.server.rest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.client.BaiduVoiceRecognitionClient;
import com.tencent.supersonic.chat.server.client.response.SynthesisResponse;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import org.json.JSONException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/supersonic/api/chat/manage", "/supersonic/openapi/chat/manage"})
public class ChatController {

    @Resource
    private ChatManageService chatService;

    @Resource
    private BaiduVoiceRecognitionClient baiduVoiceRecognitionClient;

    @PostMapping("/save")
    public Boolean save(@RequestParam(value = "chatName") String chatName,
            @RequestParam(value = "agentId", required = false) Integer agentId,
            HttpServletRequest request, HttpServletResponse response) {
        chatService.addChat(UserHolder.findUser(request, response), chatName, agentId);
        return true;
    }

    @GetMapping("/getAll")
    public List<ChatDO> getAllChats(
            @RequestParam(value = "agentId", required = false) Integer agentId,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.getAll(userName, agentId);
    }

    @PostMapping("/delete")
    public Boolean deleteChat(@RequestParam(value = "chatId") long chatId,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.deleteChat(chatId, userName);
    }

    @PostMapping("/updateChatName")
    public Boolean updateChatName(@RequestParam(value = "chatId") Long chatId,
            @RequestParam(value = "chatName") String chatName, HttpServletRequest request,
            HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.updateChatName(chatId, chatName, userName);
    }

    @PostMapping("/updateQAFeedback")
    public Boolean updateQAFeedback(@RequestParam(value = "id") Integer id,
            @RequestParam(value = "score") Integer score,
            @RequestParam(value = "feedback", required = false) String feedback) {
        return chatService.updateFeedback(id, score, feedback);
    }

    @PostMapping("/updateChatIsTop")
    public Boolean updateChatIsTop(@RequestParam(value = "chatId") Long chatId,
            @RequestParam(value = "isTop") int isTop) {
        return chatService.updateChatIsTop(chatId, isTop);
    }

    @PostMapping("/pageQueryInfo")
    public PageInfo<QueryResp> pageQueryInfo(@RequestBody PageQueryInfoReq pageQueryInfoCommand,
            @RequestParam(value = "chatId") long chatId, HttpServletRequest request,
            HttpServletResponse response) {
        pageQueryInfoCommand.setUserName(UserHolder.findUser(request, response).getName());
        return chatService.queryInfo(pageQueryInfoCommand, chatId);
    }

    /**
     * 音频识别
     *
     * @param audioFile 音频文件
     * @param format 音频类型（pcm/wav/amr/m4a）
     * @param rate 采样率（16000、8000）
     * @return 识别结果
     */
    @PostMapping("/speechRecognition")
    public String speechRecognition(@RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("format") String format, @RequestParam("rate") Integer rate) {
        // 检查音频文件是否为空
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }
        String recognitionResult = null;

        try {
            // 获取音频文件的字节数组
            byte[] audioData = audioFile.getBytes();

            // 调用语音识别服务
            recognitionResult = baiduVoiceRecognitionClient.recognize(audioData, format, rate);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        return recognitionResult;
    }

    /**
     * 音频任务合成生成
     *
     * @param text 待合成的文本，需要为UTF-8编码
     * @param format 音频格式，"mp3-16k"，"mp3-48k"，"wav"，"pcm-8k"，"pcm-16k"
     * @return 任务id
     */
    @PostMapping("/audioTaskGeneration")
    public String audioTaskGeneration(@RequestParam("text") String text,
            @RequestParam("format") String format) {
        // 检查音频文件是否为空
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("待合成的文本不能为空");
        }
        String taskId = null;

        try {
            // 调用语音合成服务
            taskId = baiduVoiceRecognitionClient.synthesis(text, format);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        return taskId;
    }

    /**
     * 音频合成查询
     *
     * @param taskIds 任务id
     */
    @PostMapping("/audioSynthesisQueries")
    public SynthesisResponse audioSynthesisQueries(@RequestBody List<String> taskIds) {
        // 检查音频文件是否为空
        if (taskIds == null || taskIds.isEmpty()) {
            throw new IllegalArgumentException("待合成的任务id不能为空");
        }

        SynthesisResponse response;
        try {
            // 调用语音合成服务
            response = baiduVoiceRecognitionClient.synthesisSearch(taskIds);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    @GetMapping("/getChatQuery/{queryId}")
    public QueryResp getChatQuery(@PathVariable("queryId") Long queryId) {
        return chatService.getChatQuery(queryId);
    }

    @DeleteMapping("/{queryId}")
    public boolean deleteChatQuery(@PathVariable(value = "queryId") Long queryId) {
        chatService.deleteQuery(queryId);
        return true;
    }

    @PostMapping("/queryShowCase")
    public ShowCaseResp queryShowCase(@RequestBody PageQueryInfoReq pageQueryInfoCommand,
            @RequestParam(value = "agentId") int agentId) {
        return chatService.queryShowCase(pageQueryInfoCommand, agentId);
    }
}
