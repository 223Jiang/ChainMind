package com.larkmt.cn.admin.filter;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkmt.core.biz.model.ReturnT;
import com.larkmt.cn.admin.core.util.I18nUtil;
import com.larkmt.cn.admin.entity.JwtUser;
import com.larkmt.cn.admin.entity.LoginUser;
import com.larkmt.cn.admin.util.JwtTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.larkmt.core.util.Constants.SPLIT_COMMA;


@Slf4j
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private ThreadLocal<Integer> rememberMe = new ThreadLocal<>();
    private AuthenticationManager authenticationManager;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        super.setFilterProcessesUrl("/larkmidtable/api/auth/login");
    }

    /**
     * 重写attemptAuthentication方法以自定义认证逻辑
     *
     * @param request  HTTP请求，用于获取登录信息
     * @param response HTTP响应，暂未在本方法中使用
     * @return Authentication 对象，如果认证成功；否则返回null
     * @throws AuthenticationException 如果认证过程中发生任何错误，将抛出此异常
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {

        // 从输入流中获取到登录的信息
        try {
            LoginUser loginUser = new ObjectMapper().readValue(request.getInputStream(), LoginUser.class);
            // 设置是否记住我
            rememberMe.set(loginUser.getRememberMe());

            // 使用认证管理器进行用户认证
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginUser.getUsername(), loginUser.getPassword(), new ArrayList<>())
            );
        } catch (IOException e) {
            // 处理输入输出异常
            logger.error("attemptAuthentication error :{}", e);
            return null;
        } catch (Exception e) {
            // 处理其他异常
            throw new RuntimeException(e);
        }
    }

    // 成功验证后调用的方法
    // 如果验证成功，就生成token并返回
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {

        JwtUser jwtUser = (JwtUser) authResult.getPrincipal();
        boolean isRemember = rememberMe.get() == 1;

        String role = "";
        Collection<? extends GrantedAuthority> authorities = jwtUser.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            role = authority.getAuthority();
        }

        String token = JwtTokenUtils.createToken(jwtUser.getId(), jwtUser.getUsername(), role, isRemember);
        response.setHeader("token", JwtTokenUtils.TOKEN_PREFIX + token);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> maps = new HashMap<>();
        maps.put("data", JwtTokenUtils.TOKEN_PREFIX + token);
        maps.put("roles", role.split(SPLIT_COMMA));
        response.getWriter().write(JSON.toJSONString(new ReturnT<>(maps)));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.toJSON(new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("login_param_invalid"))).toString());
    }
}
