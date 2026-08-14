package com.example.rbac.security.vo;

import com.example.rbac.security.common.Consts;
import com.example.rbac.security.model.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * <p>
 * 在线用户 VO
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-12 00:58
 */
@Data
public class OnlineUser {

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 生日
     */
    private Long birthday;

    /**
     * 性别，男-1，女-2
     */
    private Integer sex;

    public static OnlineUser create(User user) {
        OnlineUser onlineUser = new OnlineUser();
        BeanUtils.copyProperties(user, onlineUser);
        // 脱敏
        onlineUser.setPhone(hide(user.getPhone(), 3, 7));
        String email = user.getEmail();
        onlineUser.setEmail(hide(email, 1, email == null ? -1 : email.indexOf(Consts.SYMBOL_EMAIL)));
        return onlineUser;
    }

    /**
     * Replaces the characters in {@code [startInclude, endExclude)} with {@code *}, leaving the
     * rest untouched. Replaces hutool's {@code StrUtil.hide}, and keeps its lenient behaviour:
     * out-of-range or reversed bounds return the input unchanged rather than throwing.
     *
     * @param value        string to mask, may be null
     * @param startInclude first index to mask
     * @param endExclude   first index after the masked range
     * @return the masked string
     */
    private static String hide(String value, int startInclude, int endExclude) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int start = Math.max(startInclude, 0);
        int end = Math.min(endExclude, value.length());
        if (start >= end) {
            return value;
        }
        char[] chars = value.toCharArray();
        for (int i = start; i < end; i++) {
            chars[i] = '*';
        }
        return new String(chars);
    }
}
