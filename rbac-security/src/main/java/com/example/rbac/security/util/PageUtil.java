package com.example.rbac.security.util;

import com.example.rbac.security.common.Consts;
import com.example.rbac.security.payload.PageCondition;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;

/**
 * <p>
 * 分页工具类
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-12 18:09
 */
public class PageUtil {
    /**
     * 校验分页参数，为NULL，设置分页参数默认值
     *
     * @param condition 查询参数
     * @param clazz     类
     * @param <T>       {@link PageCondition}
     */
    public static <T extends PageCondition> void checkPageCondition(T condition, Class<T> clazz) {
        if (condition == null) {
            // BeanUtils.instantiateClass wraps reflection failures in a BeanInstantiationException,
            // which is what hutool's ReflectUtil.newInstance did with its own runtime exception.
            condition = BeanUtils.instantiateClass(clazz);
        }
        // 校验分页参数
        if (condition.getCurrentPage() == null) {
            condition.setCurrentPage(Consts.DEFAULT_CURRENT_PAGE);
        }
        if (condition.getPageSize() == null) {
            condition.setPageSize(Consts.DEFAULT_PAGE_SIZE);
        }
    }

    /**
     * 根据分页参数构建{@link PageRequest}
     *
     * @param condition 查询参数
     * @param <T>       {@link PageCondition}
     * @return {@link PageRequest}
     */
    public static <T extends PageCondition> PageRequest ofPageRequest(T condition) {
        return PageRequest.of(condition.getCurrentPage(), condition.getPageSize());
    }
}
