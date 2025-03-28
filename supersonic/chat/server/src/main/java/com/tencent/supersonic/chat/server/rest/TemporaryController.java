package com.tencent.supersonic.chat.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.chat.server.service.ExcelService;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.TemporaryReq;
import com.tencent.supersonic.chat.api.pojo.request.TemporaryUpdateReq;
import com.tencent.supersonic.chat.server.persistence.dataobject.TemporaryDO;
import com.tencent.supersonic.chat.server.persistence.repository.TemporaryRepository;
import com.tencent.supersonic.chat.server.plugin.ExcelDataImport;
import com.tencent.supersonic.common.pojo.User;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 临时表
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/27
 */
@RestController
@RequestMapping("/supersonic/api/temporary")
public class TemporaryController {

    private final TemporaryRepository temporaryRepository;

    private final ExcelService excelService;

    public TemporaryController(TemporaryRepository temporaryRepository, ExcelService excelService) {
        this.temporaryRepository = temporaryRepository;
        this.excelService = excelService;
    }

    /**
     * 上传Excel，生成表
     */
    @PostMapping("/excelDataImport")
    public String excelDataImport(@RequestBody ExcelDataImport excelDataImport,
                                  HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        excelService.processExcel(excelDataImport, user);
        return "数据导入成功";
    }

    /**
     * 查询临时表列表
     * @param temporaryReq  查询数据
     * @return              临时表数据列表
     */
    @PostMapping("/list")
    public Page<TemporaryDO> search(@RequestBody TemporaryReq temporaryReq, HttpServletRequest request,
                                    HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        temporaryReq.setUserId(user.getId());
        return temporaryRepository.search(temporaryReq);
    }

    /**
     * 更新临时表
     * @param temporaryUpdateReq    更新数据
     */
    @PostMapping("/update")
    public void update(@RequestBody TemporaryUpdateReq temporaryUpdateReq, HttpServletRequest request,
                                    HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);

        temporaryRepository.update(temporaryUpdateReq);
    }

    /**
     * 临时表删除
     * @param id    临时表id
     */
    @PostMapping("/update/{id}")
    public void update(@PathVariable Long id,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        // TODO: 临时表删除

        temporaryRepository.removeById(id);
    }
}
