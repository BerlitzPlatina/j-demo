package com.example.springaiopenai.dao.base;

import com.example.springaiopenai.annotation.Column;
import com.example.springaiopenai.annotation.Ignore;
import com.example.springaiopenai.annotation.Pk;
import com.example.springaiopenai.annotation.Table;
import com.example.springaiopenai.constant.Const;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Dao cơ sở: mini ORM dựng trên JdbcTemplate, map entity ↔ table qua annotation
 * {@link Table}, {@link Column}, {@link Pk}, {@link Ignore}.
 * </p>
 *
 * @param <T> loại entity
 * @param <P> loại khoá chính
 */
@Slf4j
public class BaseDao<T, P> {
    protected final JdbcTemplate jdbcTemplate;
    protected final Class<T> clazz;
    protected final RowMapper<T> rowMapper;

    @SuppressWarnings(value = "unchecked")
    public BaseDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        // BeanPropertyRowMapper tự map snake_case (phone_number) sang camelCase (phoneNumber)
        this.rowMapper = new BeanPropertyRowMapper<>(clazz);
    }

    /**
     * Insert chung, cột tự tăng cần đánh dấu {@link Pk}
     *
     * @param t          entity
     * @param ignoreNull có bỏ qua field null hay không
     * @return số dòng bị ảnh hưởng
     */
    protected Integer insert(T t, Boolean ignoreNull) {
        String table = getTableName();

        List<Field> filterField = getField(t, ignoreNull);
        List<String> columnList = getColumns(filterField);

        String columns = String.join(Const.SEPARATOR_COMMA, columnList);
        // Dựng placeholder
        String params = String.join(Const.SEPARATOR_COMMA, Collections.nCopies(columnList.size(), "?"));
        // Dựng giá trị
        Object[] values = filterField.stream().map(field -> getFieldValue(t, field)).toArray();

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, columns, params);
        logSql(sql, values);
        return jdbcTemplate.update(sql, values);
    }

    /**
     * Xoá theo khoá chính
     *
     * @param pk khoá chính
     * @return số dòng bị ảnh hưởng
     */
    protected Integer deleteById(P pk) {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", getTableName(), getPkColumn());
        logSql(sql, pk);
        return jdbcTemplate.update(sql, pk);
    }

    /**
     * Update theo khoá chính, cột tự tăng cần đánh dấu {@link Pk}
     *
     * @param t          entity
     * @param pk         khoá chính
     * @param ignoreNull có bỏ qua field null hay không
     * @return số dòng bị ảnh hưởng
     */
    protected Integer updateById(T t, P pk, Boolean ignoreNull) {
        List<Field> filterField = getField(t, ignoreNull);
        List<String> columnList = getColumns(filterField);

        String params = columnList.stream().map(s -> s + " = ?")
                .collect(Collectors.joining(Const.SEPARATOR_COMMA));

        // Dựng giá trị, tham số cuối là khoá chính của WHERE
        List<Object> valueList = filterField.stream().map(field -> getFieldValue(t, field))
                .collect(Collectors.toList());
        valueList.add(pk);
        Object[] values = valueList.toArray();

        String sql = String.format("UPDATE %s SET %s WHERE %s = ?", getTableName(), params, getPkColumn());
        logSql(sql, values);
        return jdbcTemplate.update(sql, values);
    }

    /**
     * Lấy một bản ghi theo khoá chính
     *
     * @param pk khoá chính
     * @return entity, hoặc {@code null} nếu không tồn tại
     */
    public T findOneById(P pk) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", getTableName(), getPkColumn());
        return findOne(sql, pk);
    }

    /**
     * Truy vấn theo entity mẫu, các field khác null được ghép thành điều kiện AND
     *
     * @param t entity mẫu
     * @return danh sách entity
     */
    public List<T> findByExample(T t) {
        List<Field> filterField = getField(t, true);
        List<String> columnList = getColumns(filterField);

        String where = columnList.stream().map(s -> " AND " + s + " = ? ").collect(Collectors.joining());
        Object[] values = filterField.stream().map(field -> getFieldValue(t, field)).toArray();

        String sql = String.format("SELECT * FROM %s WHERE 1=1 %s", getTableName(), where);
        logSql(sql, values);
        return jdbcTemplate.query(sql, rowMapper, values);
    }

    /**
     * Truy vấn danh sách bằng SQL tự viết
     *
     * @param sql    câu SQL
     * @param values tham số
     * @return danh sách entity
     */
    protected List<T> findList(String sql, Object... values) {
        logSql(sql, values);
        return jdbcTemplate.query(sql, rowMapper, values);
    }

    /**
     * Truy vấn một bản ghi bằng SQL tự viết, không throw khi rỗng
     *
     * @param sql    câu SQL
     * @param values tham số
     * @return entity, hoặc {@code null} nếu không có bản ghi nào
     */
    protected T findOne(String sql, Object... values) {
        logSql(sql, values);
        return jdbcTemplate.query(sql, rowMapper, values).stream().findFirst().orElse(null);
    }

    /**
     * Đếm bản ghi bằng SQL tự viết
     *
     * @param sql    câu SQL dạng {@code SELECT COUNT(*) ...}
     * @param values tham số
     * @return số bản ghi
     */
    protected Long count(String sql, Object... values) {
        logSql(sql, values);
        Long total = jdbcTemplate.queryForObject(sql, Long.class, values);
        return total == null ? 0L : total;
    }

    /**
     * Tên bảng của entity
     *
     * @return tên bảng đã bọc backtick
     */
    protected String getTableName() {
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        String name = tableAnnotation != null
                ? tableAnnotation.name()
                : clazz.getSimpleName().toLowerCase();
        return String.format("`%s`", name);
    }

    /**
     * Tên cột khoá chính, mặc định là {@code id} nếu không có {@link Pk}
     *
     * @return tên cột đã bọc backtick
     */
    protected String getPkColumn() {
        for (Field field : getAllFields(clazz)) {
            if (field.getAnnotation(Pk.class) != null) {
                return getColumnName(field);
            }
        }
        return "`id`";
    }

    /**
     * Danh sách cột tương ứng với danh sách field
     *
     * @param fieldList danh sách field
     * @return danh sách tên cột
     */
    private List<String> getColumns(List<Field> fieldList) {
        return fieldList.stream().map(this::getColumnName).collect(Collectors.toList());
    }

    private String getColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        String columnName = columnAnnotation != null ? columnAnnotation.name() : field.getName();
        return String.format("`%s`", columnName);
    }

    /**
     * Lấy danh sách field {@code bỏ field không có trong DB, bỏ khoá tự tăng}
     *
     * @param t          entity
     * @param ignoreNull có bỏ field null hay không
     * @return danh sách field
     */
    private List<Field> getField(T t, Boolean ignoreNull) {
        return Arrays.stream(getAllFields(t.getClass()))
                .filter(field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getAnnotation(Ignore.class) == null)
                .filter(field -> {
                    Pk pk = field.getAnnotation(Pk.class);
                    return pk == null || !pk.auto();
                })
                .filter(field -> !ignoreNull || getFieldValue(t, field) != null)
                .collect(Collectors.toList());
    }

    private Object getFieldValue(Object t, Field field) {
        try {
            field.setAccessible(true);
            return field.get(t);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Không đọc được giá trị field: " + field.getName(), e);
        }
    }

    private Field[] getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields.toArray(new Field[0]);
    }

    private void logSql(String sql, Object... values) {
        log.debug("【SQL】{}", sql);
        log.debug("【SQL】tham số: {}", Arrays.toString(values));
    }
}
