package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 处理请求前调用
     * @param request
     * @param response
     * @param handler
     * @return* @throws Exception
     * */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 得到Session
        HttpSession session = request.getSession();
        // 从Session中获取用户信息
        UserDTO user = (UserDTO)session.getAttribute("user");
        if (user == null) {
            // 如果用户信息不存在，返回401错误
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        // 将用户存入当前全局线程中
        UserHolder.saveUser(user);
        return true;
    }

    /**
     * 处理请求后调用
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    /**
     * 视图渲染完成后调用
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 认证完就清除信息
        UserHolder.removeUser();
    }
}
