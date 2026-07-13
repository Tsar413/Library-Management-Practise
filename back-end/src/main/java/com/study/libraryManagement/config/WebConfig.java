package com.study.libraryManagement.config;

import com.study.libraryManagement.interceptor.LoginInterceptor;
import com.study.libraryManagement.util.ParamsUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web 配置类
 *
 * 用于注册登录拦截器。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;


    /**
     * 注册拦截器
     *
     * 当前规则：
     * 1. 拦截 /api/users/** 下的接口
     * 2. 放行 /api/users/login
     *
     * 这样可以先测试：
     * 登录接口不需要 token
     * 查询接口需要 token
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/users/**")
                .addPathPatterns("/api/books/**")
                .addPathPatterns("/api/borrow-record/**")
                .addPathPatterns("/api/book-review/**")
                .addPathPatterns("/api/book-rating/**")
                .excludePathPatterns("/api/users/login")
                .excludePathPatterns("/api/users/registration")
                .excludePathPatterns("/Photos/**");;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /*
         * Resources root
         * addResourceHandler: Access to mapped paths
         * addResourceLocations: Absolute paths to resources
         */
        registry.addResourceHandler("/Photos/**").addResourceLocations("file:" + ParamsUtil.LOCAL_PATH + "/");
    }
}