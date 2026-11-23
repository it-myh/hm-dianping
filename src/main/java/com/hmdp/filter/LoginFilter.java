package com.hmdp.filter;

//
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//import java.io.IOException;
//@Component
//public class LoginFilter extends OncePerRequestFilter {
//    @Override
//    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
//        // 从前端拿到存储的验证码
//        String code = (String)httpServletRequest.getSession().getAttribute("loginCode");
//        // 从请求参数中获取验证码
//        String reqCode = httpServletRequest.getParameter("code");
//        // 验证码是否一致
//        if(!code.equals(reqCode)){
//            return;
//        }
//        // 验证码一致，放行
//        filterChain.doFilter(httpServletRequest, httpServletResponse);
//    }
//}
