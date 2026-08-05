package com.dyrnq.distops;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.noear.wood.wrap.TypeConverter;

/**
 * Override wood's {@link TypeConverter} to accept MySQL-style "yyyy-MM-dd HH:mm:ss"
 * {@code LocalDateTime} strings (space separator, no 'T') in addition to the
 * ISO-8601 default.
 *
 * <p>Wood 1.0.7~1.4.8 calls {@code LocalDateTime.parse(s)} unconditionally for
 * {@code String -> LocalDateTime}, which only accepts ISO-8601. Many JDBC drivers
 * (MySQL, H2, ...) return {@code DATETIME}/{@code TIMESTAMP} columns as
 * {@code "2026-08-05 12:34:56"} strings; those would throw
 * {@link java.time.format.DateTimeTimeParseException} and the row would fail to
 * deserialize. This subclass adds the MySQL fallback before delegating.
 *
 * <p>Installed once at startup via
 * {@code WoodConfig.typeConverter = new WoodTypeConverter();} in {@link WebApp}.
 */
public class WoodTypeConverter extends TypeConverter {

    private static final DateTimeFormatter MYSQL_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object convert(Object val, Class<?> target) throws SQLException, IOException {
        if (target == LocalDateTime.class && val instanceof String) {
            String s = (String) val;
            if (!s.contains("T")) {
                return LocalDateTime.parse(s, MYSQL_DATETIME);
            }
        }
        return super.convert(val, target);
    }
}
