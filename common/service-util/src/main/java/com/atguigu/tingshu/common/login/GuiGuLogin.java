package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

/**
 * 1 获取当前正在访问系统的用户的id
 *      属性：要求是否必须登录（true） 要求必须登录
 *
 *   @Target：注解使用的位置：指定方法，类（接口），属性，构造器参数
 *   @Retention：生命周期 SOURCE源码时有效 CLASS编译时候生效，RUNTIME运行时有效
 *   @Inherited 该注解是否可以被继承
 *   @Documented 是否生成文档 java-doc
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuLogin {
    //是否要求用户必须登录：默认必须登录
    boolean required() default true;
}
