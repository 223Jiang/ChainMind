package com.larkmt.cn.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.larkmt.cn.admin.entity.JobUser;
import com.larkmt.core.biz.model.ReturnT;

/**
 * @author JiangWeiWei
 */
public interface JobUserService extends IService<JobUser> {
    ReturnT<String> addJobUser(JobUser jobUser);

    ReturnT<String> updateJobUser(JobUser jobUser);

    int removeJobUser(int id);

    void resetPassword(String correlationId, String newPassword);

    JobUser searchByName(String username);
}