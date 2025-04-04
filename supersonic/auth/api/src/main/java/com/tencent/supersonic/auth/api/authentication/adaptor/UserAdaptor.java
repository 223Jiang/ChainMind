package com.tencent.supersonic.auth.api.authentication.adaptor;

import javax.servlet.http.HttpServletRequest;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;
import java.util.Set;

/** UserAdaptor defines some interfaces for obtaining user and organization information */
public interface UserAdaptor {

    List<String> getUserNames();

    List<User> getUserList();

    List<Organization> getOrganizationTree();

    void register(UserReq userReq);

    String login(UserReq userReq, HttpServletRequest request);

    String login(UserReq userReq, String appKey);

    List<User> getUserByOrg(String key);

    Set<String> getUserAllOrgId(String userName);

    String getPassword(String userName);

    /**
     *
     * @param userName
     * @param password
     * @param newPassword
     * @return 返回correlationId
     */
    String resetPassword(String userName, String password, String newPassword);

    UserToken generateToken(String name, String userName, long expireTime);

    void deleteUserToken(Long id);

    UserToken getUserToken(Long id);

    List<UserToken> getUserTokens(String userName);

    void modifyTheUser(UserReq userReq);

    int deleteTheUser(String correlationId);
}
