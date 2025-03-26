package com.tencent.supersonic.auth.authentication.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.authentication.service.UserServiceImpl;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.S2ThreadContext;
import com.tencent.supersonic.common.util.ThreadContext;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_IS_ADMIN;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_DISPLAY_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_EMAIL;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_PASSWORD;

@Slf4j
public class DefaultAuthenticationInterceptor extends AuthenticationInterceptor {

    /**
     * 在请求处理之前进行预处理，主要用于权限验证和用户信息的设置
     *
     * @param request 请求对象，用于获取请求信息
     * @param response 响应对象，用于处理响应结果
     * @param handler 当前请求的处理器，用于判断是否应用权限验证
     * @return 如果验证通过或无需验证，则返回true，否则抛出AccessException异常
     * @throws AccessException 当验证失败时抛出此异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws AccessException {
        // 获取认证配置、用户服务、Token服务和线程上下文对象
        authenticationConfig = ContextUtils.getBean(AuthenticationConfig.class);
        userServiceImpl = ContextUtils.getBean(UserServiceImpl.class);
        tokenService = ContextUtils.getBean(TokenService.class);
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);

        // 如果认证功能未启用，则设置假用户并允许请求通过
        if (!authenticationConfig.isEnabled()) {
            setFakerUser(request);
            return true;
        }

        // 如果是内部请求或App请求，则设置假用户并允许请求通过
        if (isInternalRequest(request) || isAppRequest(request)) {
            setFakerUser(request);
            return true;
        }

        // 如果当前处理器是HandlerMethod类型，判断方法上是否有AuthenticationIgnore注解，有则允许请求通过
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            AuthenticationIgnore ignore = method.getAnnotation(AuthenticationIgnore.class);
            if (ignore != null) {
                return true;
            }
        }

        // 获取请求的URI，判断是否在包含的URI列表中，不在则允许请求通过
        String uri = request.getServletPath();
        if (!isIncludedUri(uri)) {
            return true;
        }

        // 判断URI是否在排除的URI列表中，是则允许请求通过
        if (isExcludedUri(uri)) {
            return true;
        }

        // 从请求中获取用户信息，如果获取成功，则设置线程上下文并允许请求通过
        UserWithPassword user = getUserWithPassword(request);
        if (user != null) {
            setContext(user.getName(), request);
            return true;
        }

        // 如果以上所有条件都不满足，则抛出AccessException异常，表示验证失败
        throw new AccessException("authentication failed, please login");
    }

    /**
     * 为请求设置伪造的用户信息 此方法用于在测试或特定场景下，模拟一个管理员用户的身份 通过生成一个管理员令牌，并将其设置到请求的头部，以伪造用户登录的状态
     * 同时，将默认用户的名称设置到上下文中，以便后续处理可以识别或使用该用户信息
     *
     * @param request HTTP请求对象，用于生成令牌并设置头部信息
     */
    private void setFakerUser(HttpServletRequest request) {
        // 生成管理员令牌
        String token = generateAdminToken(request);
        // 通过反射设置请求头部，头部键名来自认证配置
        reflectSetParam(request, authenticationConfig.getTokenHttpHeaderKey(), token);
        // 设置上下文，包括默认用户名和请求对象，以便后续处理可以访问这些信息
        setContext(User.getDefaultUser().getName(), request);
    }


    /**
     * 设置当前线程的上下文信息 此方法用于初始化一个ThreadContext对象，并将其设置到当前线程的特定变量s2ThreadContext中
     * 主要包括用户信息和认证令牌，以便在后续的处理中能够轻松访问这些信息
     *
     * @param userName 用户名，用于标识用户
     * @param request HTTP请求对象，从中可以提取出请求头信息，包括认证令牌
     */
    private void setContext(String userName, HttpServletRequest request) {
        // 构建ThreadContext对象，包含令牌和用户名
        ThreadContext threadContext = ThreadContext.builder()
                .token(request.getHeader(authenticationConfig.getTokenHttpHeaderKey()))
                .userName(userName).build();
        // 将构建好的ThreadContext对象设置到当前线程的特定变量s2ThreadContext中
        s2ThreadContext.set(threadContext);
    }

    /**
     * 为管理员生成令牌
     *
     * 此方法用于创建并返回一个管理员用户的令牌它首先创建一个表示管理员的UserWithPassword对象，
     * 设置其属性，然后调用tokenService的generateToken方法生成令牌这个过程涉及到将管理员信息转换为
     * 用户对象，这是通过UserWithPassword类的convert静态方法完成的
     *
     * @param request HttpServletRequest对象，包含生成令牌时可能需要的请求信息
     * @return 生成的管理员令牌字符串
     */
    public String generateAdminToken(HttpServletRequest request) {
        // 创建一个表示管理员的UserWithPassword对象
        UserWithPassword admin = new UserWithPassword("admin");
        // 设置管理员的ID
        admin.setId(1L);
        // 设置管理员的用户名
        admin.setName("admin");
        // 设置管理员的密码，这里使用的是加密后的字符串
        admin.setPassword("c3VwZXJzb25iYWNrQGJpY29tbWVudC05dXJlZ2VudHJ5QWNjZXNzMS5pbmZv");
        // 设置管理员的显示名称
        admin.setDisplayName("admin");
        // 设置管理员的权限标志，1表示是管理员
        admin.setIsAdmin(1);
        // 使用UserWithPassword类的convert静态方法将管理员信息转换为用户对象，并生成令牌
        return tokenService.generateToken(UserWithPassword.convert(admin), request);
    }

    /**
     * 从HTTP请求中提取用户信息，包括密码 此方法主要用于从请求中的令牌提取用户详细信息，包括用户名、电子邮件、显示名称和密码
     * 它依赖于令牌服务来解析请求中的声明，并根据这些声明构建一个包含用户信息的对象
     *
     * @param request HTTP请求对象，用于提取用户信息的令牌
     * @return 返回一个UserWithPassword对象，包含用户详细信息和密码如果令牌无效或解析失败，则返回null
     */
    public UserWithPassword getUserWithPassword(HttpServletRequest request) {
        // 尝试从请求中获取用户声明，如果无法获取，则返回null
        final Optional<Claims> claimsOptional = tokenService.getClaims(request);
        if (!claimsOptional.isPresent()) {
            return null;
        }

        Claims claims = claimsOptional.get();

        // 从声明中提取用户ID、用户名、电子邮件、显示名称、密码和管理员状态
        Long userId = Long.parseLong(claims.getOrDefault(TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(TOKEN_USER_DISPLAY_NAME));
        String password = String.valueOf(claims.get(TOKEN_USER_PASSWORD));
        Integer isAdmin = claims.get(TOKEN_IS_ADMIN) == null ? 0
                : Integer.parseInt(claims.get(TOKEN_IS_ADMIN).toString());

        // 使用提取的信息创建并返回UserWithPassword对象
        return UserWithPassword.get(userId, userName, displayName, email, password, isAdmin);
    }
}
