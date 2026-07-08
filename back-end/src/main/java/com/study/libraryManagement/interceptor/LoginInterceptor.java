package com.study.libraryManagement.interceptor;

import com.study.libraryManagement.util.TokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 *
 * 作用：
 * 1. 从请求头 Authorization 中获取 token
 * 2. 根据 token 查询 userId
 * 3. 如果 token 正确，把 userId 放入 request
 * 4. Controller 后续可以从 request 中取出 userId
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * Controller 方法执行前触发
     *
     * 返回 true：放行
     * 返回 false：拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. 从请求头中获取 token
        String token = request.getHeader("Authorization");

        // 2. 判断 token 是否为空
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 3. 兼容 Authorization: Bearer xxxxx 的写法
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 4. 根据 token 获取 userId
        Long userId = TokenUtil.getUserIdByToken(token);

        // 5. token 无效
        if (userId == null) {
            response.setStatus(401);
            return false;
        }

        // 6. 把 userId 放入 request
        // 后面的 Controller 可以通过 request.getAttribute("userId") 获取
        request.setAttribute("userId", userId);

        return true;
    }
}
