package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    /**
     *
     * 发送验证码
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        // 生成六位验证码
        String code = RandomUtil.randomNumbers(6);
        // 保存验证码到session（淘汰方案）
//        session.setAttribute("loginCode", code);
        // 使用Redis保存验证码(有效期2分钟)
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 发送验证码到手机号
        log.info("发送验证码到手机号：{}，验证码：{}", phone, code);
        return Result.ok();
    }

    /**
     * 登录注册合一功能
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 检验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        // 校验验证码
        if(RegexUtils.isCodeInvalid(loginForm.getCode())){
            return Result.fail("验证码格式错误");
        }
        // 验证验证码的是否一致（淘汰方案）
//        String loginCode = (String)session.getAttribute("loginCode");
        // 从Redis中获取验证码
        String loginCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        // 校验验证码是否一致
        if(loginCode.isEmpty() || !loginCode.equals(loginForm.getCode())){
            return Result.fail("验证码错误");
        }
        // 更具手机号查询用户
        User user = query().eq("phone", phone).one();
        // 看用户是否存在
        if(user == null){
            // 不存在，注册用户,并保存用户
            user = saveUserWithPhone(phone);
        }
        // 保存用户的信息到Session中（淘汰方案）
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        // 保存用户的信息到Redis中
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = new HashMap<>();
        map.put("id", userDTO.getId());
        map.put("nickName", userDTO.getNickName());
        map.put("icon", userDTO.getIcon());
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + user.getId(), map);
        return Result.ok();
    }

    private User saveUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 保存用户
        save(user);
        return user;
    }
}