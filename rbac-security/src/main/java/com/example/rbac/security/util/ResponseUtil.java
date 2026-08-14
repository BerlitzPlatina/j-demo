package com.example.rbac.security.util;

import com.example.rbac.security.common.ApiResponse;
import com.example.rbac.security.common.BaseException;
import com.example.rbac.security.common.IStatus;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 * Response 通用工具类
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-07 17:37
 */
@Slf4j
public class ResponseUtil {

    /**
     * Jackson keeps null fields by default, which is what the previous hutool call asked for by
     * passing {@code ignoreNullValue = false}. Shared because ObjectMapper is thread safe.
     */
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    /**
     * 往 response 写出 json
     *
     * @param response 响应
     * @param status   状态
     * @param data     返回数据
     */
    public static void renderJson(HttpServletResponse response, IStatus status, Object data) {
        renderJson(response, ApiResponse.ofStatus(status, data));
    }

    /**
     * 往 response 写出 json
     *
     * @param response  响应
     * @param exception 异常
     */
    public static void renderJson(HttpServletResponse response, BaseException exception) {
        renderJson(response, ApiResponse.ofException(exception));
    }

    /**
     * Writes the body as JSON with the CORS and content type headers the two callers above share.
     *
     * @param response 响应
     * @param body     返回内容
     */
    private static void renderJson(HttpServletResponse response, ApiResponse body) {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "*");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(200);

            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
        } catch (IOException e) {
            log.error("Response写出JSON异常，", e);
        }
    }
}
