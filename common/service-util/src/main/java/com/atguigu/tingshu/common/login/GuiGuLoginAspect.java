package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证校验的切面类：对自定义注解修饰的放法增强，获取登录用户的id，执行不同业务
 * 1 声明切面类产生对象
 * 2 声明通知
 * 3 声明切入点（对注解进行增强）
 * 4 完善认证通知代码，尝试获取登录用户id
 */
@Slf4j
@Aspect //声明切面
@Component //将切面对象注册到ioc容器
public class GuiGuLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @param joinPoint 切入点方法对象
     * @return guiGuLogin 自定义注解对象
     */
    @SneakyThrows //自动对方法加try catch
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object guiguLoginAspect(ProceedingJoinPoint joinPoint, GuiGuLogin guiGuLogin) {
        Object object = new Object();
        log.info("前置通知执行了。。。");
        //1 尝试从请求对象中获取用户token（请求头）
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //RequestAttributes是接口， ServletRequestAttributes是接口实现类
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;

        HttpServletRequest request = sra.getRequest();
        HttpServletResponse response = sra.getResponse();
        //2 根基token获取用户信息，id，名称
        //2.1 获取用户token
        String token = request.getHeader("token");
        //2.2 拼接登录后存入redis中的key
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(loginKey);
        if (guiGuLogin.required()){
            //要求登录才能访问
            if (userInfo == null) {
                throw  new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //3 将用户信息隐式传入，在当前线程声生命周期获取到用户信息
        if (userInfo != null){
            //将用户id，名称存入ThreadLocal中，以便随时取值
            AuthContextHolder.setUserId(userInfo.getId());
            AuthContextHolder.setUsername(userInfo.getNickname());
        }

        //执行目标方法->切入点方法（被增强的方法）
        object = joinPoint.proceed();
        log.info("后置通知执行了。。。");
        //避免出现ThreadLocal内存泄漏从而导致内存溢出，必须将ThreadLocal清理数据
        AuthContextHolder.removeUserId();
        AuthContextHolder.removeUsername();
        return object;
    }
}
