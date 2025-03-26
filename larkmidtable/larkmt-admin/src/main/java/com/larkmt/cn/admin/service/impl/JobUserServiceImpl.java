package com.larkmt.cn.admin.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.larkmt.cn.admin.core.util.I18nUtil;
import com.larkmt.cn.admin.entity.JobUser;
import com.larkmt.cn.admin.mapper.JobUserMapper;
import com.larkmt.cn.admin.service.JobUserService;
import com.larkmt.cn.admin.util.AESSupersonicUtil;
import com.larkmt.core.biz.model.ReturnT;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

import static com.larkmt.core.biz.model.ReturnT.FAIL_CODE;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/11
 */
@DubboService
public class JobUserServiceImpl extends ServiceImpl<JobUserMapper, JobUser> implements JobUserService {
    @Resource
    private JobUserMapper jobUserMapper;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public ReturnT<String> addJobUser(JobUser jobUser) {
        // valid username
        if (!StringUtils.hasText(jobUser.getUsername())) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username"));
        }
        jobUser.setUsername(jobUser.getUsername().trim());
        if (!(jobUser.getUsername().length() >= 4 && jobUser.getUsername().length() <= 20)) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
        }
        // valid password
        if (!StringUtils.hasText(jobUser.getPassword())) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_password"));
        }
        jobUser.setPassword(jobUser.getPassword().trim());
        if (!(jobUser.getPassword().length() >= 4 && jobUser.getPassword().length() <= 20)) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
        }
        jobUser.setPassword(bCryptPasswordEncoder.encode(jobUser.getPassword()));

        // check repeat
        JobUser existUser = jobUserMapper.loadByUserName(jobUser.getUsername());
        if (existUser != null) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("user_username_repeat"));
        }

        // write
        jobUserMapper.save(jobUser);

        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> updateJobUser(JobUser jobUser) {
        if (StringUtils.hasText(jobUser.getPassword())) {
            String pwd = jobUser.getPassword().trim();
            if (StrUtil.isBlank(pwd)) {
                return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_no_blank") + "密码");
            }

            if (!(pwd.length() >= 4 && pwd.length() <= 20)) {
                return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_length_limit") + "[4-20]");
            }

            jobUser.setPassword(bCryptPasswordEncoder.encode(pwd));
        } else {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("system_no_blank") + "密码");
        }
        // write
        jobUserMapper.update(jobUser);
        return ReturnT.SUCCESS;
    }

    @Override
    public int removeJobUser(int id) {
        JobUser jobUser = this.getById(id);

        return jobUserMapper.delete(id);
    }

    @Override
    public void resetPassword(String correlationId, String newPassword) {
        newPassword = bCryptPasswordEncoder.encode(newPassword);

        jobUserMapper.resetPassword(correlationId, newPassword);
    }

    @Override
    public JobUser searchByName(String username) {
        return jobUserMapper.searchByName(username);
    }
}
