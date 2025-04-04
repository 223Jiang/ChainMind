package com.tencent.supersonic.headless.server.facade.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataSetReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/supersonic/api/semantic/query")
@Slf4j
public class DataSetQueryApiController {

    @Autowired
    private DataSetService dataSetService;
    @Autowired
    private SemanticLayerService semanticLayerService;

    @PostMapping("/dataSet")
    public Object queryByDataSet(@RequestBody QueryDataSetReq queryDataSetReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        SemanticQueryReq queryReq = dataSetService.convert(queryDataSetReq);
        return semanticLayerService.queryByReq(queryReq, user);
    }
}
