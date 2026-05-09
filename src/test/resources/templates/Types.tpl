put(Types.BIGINT, "Long", "long");
put(Types.BINARY, "byte[]", "byte[]");
put(Types.BIT, "Boolean", "boolean");
put(Types.BLOB, "byte[]", "byte[]");
put(Types.BOOLEAN, "Boolean", "boolean");
put(Types.CHAR, "String", "String");
put(Types.CLOB, "String", "String");
put(Types.DATE, "Date", "Date");
put(Types.DECIMAL, "BigDecimal", "BigDecimal");
put(Types.DOUBLE, "Double", "double");
put(Types.FLOAT, "Float", "float");
put(Types.INTEGER, "Integer", "int");
put(Types.JAVA_OBJECT, "Object", "Object");
put(Types.LONGNVARCHAR, "String", "String");
put(Types.LONGVARBINARY, "byte[]", "byte[]");
put(Types.LONGVARCHAR, "String", "String");
put(Types.NCHAR, "String", "String");
put(Types.NVARCHAR, "String", "String");
put(Types.NCLOB, "String", "String");
// 根据长度制定Integer，或者Double
put(Types.NUMERIC, NUMERIC, NUMERIC);
put(Types.OTHER, "Object", "Object");
put(Types.REAL, "Float", "float");

put(Types.SMALLINT, "Integer", "int");
put(Types.SQLXML, "SQLXML", "SQLXML");
put(Types.TIME, "Date", "Date");
put(Types.TIMESTAMP, "Date", "Date");
put(Types.TINYINT, "Integer", "int");
put(Types.VARBINARY, "byte[]", "byte[]");
put(Types.VARCHAR, "String", "String");

// jdk 8 support
put(Types.TIMESTAMP_WITH_TIMEZONE, "Date", "Date");
put(Types.TIME_WITH_TIMEZONE, "Date", "Date");
put(Types.DATETIME, "LocalDateTime", "LocalDateTime");