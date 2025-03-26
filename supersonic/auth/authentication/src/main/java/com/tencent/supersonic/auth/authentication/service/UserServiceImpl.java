package com.tencent.supersonic.auth.authentication.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.IdUtil;
import com.larkmt.cn.admin.entity.JobUser;
import com.larkmt.cn.admin.service.JobUserService;
import com.larkmt.cn.admin.util.AESSupersonicUtil;
import com.larkmt.cn.admin.util.JwtTokenUtils;
import com.larkmt.core.biz.model.ReturnT;
import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.authentication.utils.ComponentFactory;
import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.SystemConfigService;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

@DubboService
public class UserServiceImpl implements UserService {

    private final SystemConfigService sysParameterService;

    @DubboReference
    private JobUserService jobUserService;

    public UserServiceImpl(SystemConfigService sysParameterService) {
        this.sysParameterService = sysParameterService;
    }

    @Override
    public User getCurrentUser(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        if (user != null) {
            SystemConfig systemConfig = sysParameterService.getSystemConfig();
            if (!CollectionUtils.isEmpty(systemConfig.getAdmins())
                    && systemConfig.getAdmins().contains(user.getName())) {
                user.setIsAdmin(1);
            }
        }
        return user;
    }

    @Override
    public List<String> getUserNames() {
        return ComponentFactory.getUserAdaptor().getUserNames();
    }

    @Override
    public List<User> getUserList() {
        return ComponentFactory.getUserAdaptor().getUserList();
    }

    @Override
    public Set<String> getUserAllOrgId(String userName) {
        return ComponentFactory.getUserAdaptor().getUserAllOrgId(userName);
    }

    @Override
    public List<User> getUserByOrg(String key) {
        return ComponentFactory.getUserAdaptor().getUserByOrg(key);
    }

    @Override
    public List<Organization> getOrganizationTree() {
        return ComponentFactory.getUserAdaptor().getOrganizationTree();
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void register(UserReq userReq) {
        String correlationId = IdUtil.getSnowflake().nextIdStr();

        // -----远程larkmidtabled用户创建
        JobUser jobUser = new JobUser();
        jobUser.setUsername(userReq.getName());
        String password;
        try {
            password = AESSupersonicUtil.decryptPassword(userReq.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        jobUser.setPassword(password);
        jobUser.setCorrelationId(correlationId);
        // 默认为普通用户
        jobUser.setRole("ROLE_USER");
        ReturnT<String> returnT = jobUserService.addJobUser(jobUser);
        if (ReturnT.SUCCESS_CODE != returnT.getCode()) {
            throw new RuntimeException(returnT.getMsg());
        }
        // -----远程larkmidtabled用户创建

        userReq.setCorrelationId(correlationId);
        ComponentFactory.getUserAdaptor().register(userReq);
    }

    @Override
    public String login(UserReq userReq, HttpServletRequest request) {
        String biToken = ComponentFactory.getUserAdaptor().login(userReq, request);

        // 获取larkmidtabled的token
        JobUser jobUser = jobUserService.searchByName(userReq.getName());
        if (jobUser == null) {
            throw new RuntimeException("larkmidtabled数据用户数据没有同步！");
        }
        String larkmidtabledToken = JwtTokenUtils.createToken(jobUser.getId(),
                jobUser.getUsername(), jobUser.getRole(), true);

        // 获取的两个token通过‘，’号分割
        return biToken + "，" + larkmidtabledToken;
    }

    @Override
    public String login(UserReq userReq, String appKey) {
        return ComponentFactory.getUserAdaptor().login(userReq, appKey);
    }

    @Override
    public String getPassword(String userName) {
        return ComponentFactory.getUserAdaptor().getPassword(userName);
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void resetPassword(String userName, String password, String newPassword) {
        String correlationId =
                ComponentFactory.getUserAdaptor().resetPassword(userName, password, newPassword);
        try {
            newPassword = AESSupersonicUtil.decryptPassword(newPassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // -----远程larkmidtabled用户修改密码
        jobUserService.resetPassword(correlationId, newPassword);
    }

    @Override
    public UserToken generateToken(String name, String userName, long expireTime) {
        return ComponentFactory.getUserAdaptor().generateToken(name, userName, expireTime);
    }

    @Override
    public List<UserToken> getUserTokens(String userName) {
        return ComponentFactory.getUserAdaptor().getUserTokens(userName);
    }

    @Override
    public UserToken getUserToken(Long id) {
        return ComponentFactory.getUserAdaptor().getUserToken(id);
    }

    @Override
    public void deleteUserToken(Long id) {
        ComponentFactory.getUserAdaptor().deleteUserToken(id);
    }

    @Override
    public void modifyTheUser(UserReq userReq) {
        ComponentFactory.getUserAdaptor().modifyTheUser(userReq);
    }

    @Override
    public int deleteTheUser(String correlationId) {
        return ComponentFactory.getUserAdaptor().deleteTheUser(correlationId);
    }
}
