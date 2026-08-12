package com.example.orm.jdbctemplate.dao.base;

import com.example.orm.jdbctemplate.annotation.Column;
import com.example.orm.jdbctemplate.annotation.Ignore;
import com.example.orm.jdbctemplate.annotation.Pk;
import com.example.orm.jdbctemplate.annotation.Table;
import com.example.orm.jdbctemplate.constant.Const;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Dao基类
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-10-15 11:28
 */
@Slf4j
public class BaseDao<T, P> {
    private JdbcTemplate jdbcTemplate;
    private Class<T> clazz;

    @SuppressWarnings(value = "unchecked")
    public BaseDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * 通用插入，自增列需要添加 {@link Pk} 注解
     *
     * @param t          对象
     * @param ignoreNull 是否忽略 null 值
     * @return 操作的行数
     */
    protected Integer insert(T t, Boolean ignoreNull) {
        String table = getTableName(t);

        List<Field> filterField = getField(t, ignoreNull);

        List<String> columnList = getColumns(filterField);

        String columns = String.join(Const.SEPARATOR_COMMA, columnList);

        // 构造占位符
        String params = String.join(Const.SEPARATOR_COMMA, Collections.nCopies(columnList.size(), "?"));

        // 构造值
        Object[] values = filterField.stream().map(field -> getFieldValue(t, field)).toArray();

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, columns, params);
        log.debug("【执行SQL】SQL：{}", sql);
        log.debug("【执行SQL】参数：{}", Arrays.toString(values));
        return jdbcTemplate.update(sql, values);
    }

    /**
     * 通用根据主键删除
     *
     * @param pk 主键
     * @return 影响行数
     */
    protected Integer deleteById(P pk) {
        String tableName = getTableName();
        String sql = String.format("DELETE FROM %s where id = ?", tableName);
        log.debug("【执行SQL】SQL：{}", sql);
        log.debug("【执行SQL】参数：{}", pk);
        return jdbcTemplate.update(sql, pk);
    }

    /**
     * 通用根据主键更新，自增列需要添加 {@link Pk} 注解
     *
     * @param t          对象
     * @param pk         主键
     * @param ignoreNull 是否忽略 null 值
     * @return 操作的行数
     */
    protected Integer updateById(T t, P pk, Boolean ignoreNull) {
        String tableName = getTableName(t);

        List<Field> filterField = getField(t, ignoreNull);

        List<String> columnList = getColumns(filterField);

        List<String> columns = columnList.stream().map(s -> s + " = ?")
                .collect(Collectors.toList());
        String params = String.join(Const.SEPARATOR_COMMA, columns);

        // 构造值
        List<Object> valueList = filterField.stream().map(field -> getFieldValue(t, field))
                .collect(Collectors.toList());
        valueList.add(pk);

        Object[] values = valueList.toArray();

        String sql = String.format("UPDATE %s SET %s where id = ?", tableName, params);
        log.debug("【执行SQL】SQL：{}", sql);
        log.debug("【执行SQL】参数：{}", Arrays.toString(values));
        return jdbcTemplate.update(sql, values);
    }

    /**
     * 通用根据主键查询单条记录
     *
     * @param pk 主键
     * @return 单条记录
     */
    public T findOneById(P pk) {
        String tableName = getTableName();
        String sql = String.format("SELECT * FROM %s where id = ?", tableName);
        RowMapper<T> rowMapper = new BeanPropertyRowMapper<>(clazz);
        log.debug("【执行SQL】SQL：{}", sql);
        log.debug("【执行SQL】参数：{}", pk);
        return jdbcTemplate.queryForObject(sql, rowMapper, pk);
    }

    /**
     * 根据对象查询
     *
     * @param t 查询条件
     * @return 对象列表
     */
    public List<T> findByExample(T t) {
        String tableName = getTableName(t);
        List<Field> filterField = getField(t, true);
        List<String> columnList = getColumns(filterField);

        List<String> columns = columnList.stream().map(s -> " and " + s + " = ? ").collect(Collectors.toList());

        String where = String.join(" ", columns);
        // 构造值
        Object[] values = filterField.stream().map(field -> getFieldValue(t, field)).toArray();

        String sql = String.format("SELECT * FROM %s where 1=1 %s", tableName,
                (where == null || where.isEmpty()) ? "" : where);
        log.debug("【执行SQL】SQL：{}", sql);
        log.debug("【执行SQL】参数：{}", Arrays.toString(values));
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql, values);
        List<T> ret = new ArrayList<>();
        maps.forEach(map -> ret.add(fillBeanWithMap(map)));
        return ret;
    }

    /**
     * 获取表名
     *
     * @param t 对象
     * @return 表名
     */
    private String getTableName(T t) {
        Table tableAnnotation = t.getClass().getAnnotation(Table.class);
        if (tableAnnotation != null) {
            return String.format("`%s`", tableAnnotation.name());
        } else {
            return String.format("`%s`", t.getClass().getName().toLowerCase());
        }
    }

    /**
     * 获取表名
     *
     * @return 表名
     */
    private String getTableName() {
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            return String.format("`%s`", tableAnnotation.name());
        } else {
            return String.format("`%s`", clazz.getName().toLowerCase());
        }
    }

    /**
     * 获取列
     *
     * @param fieldList 字段列表
     * @return 列信息列表
     */
    private List<String> getColumns(List<Field> fieldList) {
        // 构造列
        List<String> columnList = new ArrayList<>();
        for (Field field : fieldList) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            String columnName;
            if (columnAnnotation != null) {
                columnName = columnAnnotation.name();
            } else {
                columnName = field.getName();
            }
            columnList.add(String.format("`%s`", columnName));
        }
        return columnList;
    }

    /**
     * 获取字段列表 {@code 过滤数据库中不存在的字段，以及自增列}
     *
     * @param t          对象
     * @param ignoreNull 是否忽略空值
     * @return 字段列表
     */
    private List<Field> getField(T t, Boolean ignoreNull) {
        // 获取所有字段，包含父类中的字段
        Field[] fields = getAllFields(t.getClass());

        // 过滤数据库中不存在的字段，以及自增列
        List<Field> filterField;
        Stream<Field> fieldStream = Arrays.stream(fields)
                .filter(field -> field.getAnnotation(Ignore.class) == null
                        || field.getAnnotation(Pk.class) == null);

        // 是否过滤字段值为null的字段
        if (ignoreNull) {
            filterField = fieldStream.filter(field -> getFieldValue(t, field) != null)
                    .collect(Collectors.toList());
        } else {
            filterField = fieldStream.collect(Collectors.toList());
        }
        return filterField;
    }

    private Object getFieldValue(T t, Field field) {
        try {
            field.setAccessible(true);
            return field.get(t);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + field.getName(), e);
        }
    }

    private Field[] getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields.toArray(new Field[0]);
    }

    private T fillBeanWithMap(Map<String, Object> map) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            BeanWrapper beanWrapper = new BeanWrapperImpl(instance);
            map.forEach((key, value) -> {
                try {
                    beanWrapper.setPropertyValue(key, value);
                } catch (Exception ignored) {
                }
            });
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

}
