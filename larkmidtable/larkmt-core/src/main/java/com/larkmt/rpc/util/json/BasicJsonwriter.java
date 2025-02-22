package com.larkmt.rpc.util.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 *
 * @Author: LarkMidTable
 * @Date: 2020/9/16 11:14
 * @Description: JsonWriter的父类
 **/
public class BasicJsonwriter {
	private static Logger logger = LoggerFactory.getLogger(BasicJsonwriter.class);


	private static final String STR_SLASH = "\"";
	private static final String STR_SLASH_STR = "\":";
	private static final String STR_COMMA = ",";
	private static final String STR_OBJECT_LEFT = "{";
	private static final String STR_OBJECT_RIGHT = "}";
	private static final String STR_ARRAY_LEFT = "[";
	private static final String STR_ARRAY_RIGHT = "]";

	private static final Map<String, Field[]> cacheFields = new HashMap<>();


	/**
	 *
	 * @param object
	 * @return 将JSON转换为String字符串
	 */
	public String toJson(Object object) {
		StringBuilder json = new StringBuilder();
		try {
			writeObjItem(null, object, json);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		String str = json.toString();
		if (str.contains("\n")) {
			str = str.replaceAll("\\n", "\\\\n");
		}
		if (str.contains("\t")) {
			str = str.replaceAll("\\t", "\\\\t");
		}
		if (str.contains("\r")) {
			str = str.replaceAll("\\r", "\\\\r");
		}
		return str;
	}

	/**
	 * 返回json格式的键值对
	 * @param key   :键
	 * @param value :值
	 * @param json  :返回值 "key":value or value
	 */
	private void writeObjItem(String key, Object value, StringBuilder json) {

		// "key:"
		if (key != null) {
			json.append(STR_SLASH).append(key).append(STR_SLASH_STR);
		}

		// val
		if (value == null) {
			json.append("null");
		} else if (value instanceof String || value instanceof Byte || value instanceof CharSequence) {
			// string
			json.append(STR_SLASH).append(value.toString()).append(STR_SLASH);
		} else if (value instanceof Boolean || value instanceof Short || value instanceof Integer
				|| value instanceof Long || value instanceof Float || value instanceof Double) {
			// number
			json.append(value);
		} else if (value instanceof Object[] || value instanceof Collection) {
			// collection | array     //  Array.getLength(array);   // Array.get(array, i);
			Collection valueColl = null;
			if (value instanceof Object[]) {
				Object[] valueArr = (Object[]) value;
				valueColl = Arrays.asList(valueArr);
			} else if (value instanceof Collection) {
				valueColl = (Collection) value;
			}

			json.append(STR_ARRAY_LEFT);
			if (valueColl.size() > 0) {
				for (Object obj : valueColl) {
					writeObjItem(null, obj, json);
					json.append(STR_COMMA);
				}
				json.delete(json.length() - 1, json.length());
			}
			json.append(STR_ARRAY_RIGHT);

		} else if (value instanceof Map) {
			// map

			Map<?, ?> valueMap = (Map<?, ?>) value;

			json.append(STR_OBJECT_LEFT);
			if (!valueMap.isEmpty()) {
				Set<?> keys = valueMap.keySet();
				for (Object valueMapItemKey : keys) {
					writeObjItem(valueMapItemKey.toString(), valueMap.get(valueMapItemKey), json);
					json.append(STR_COMMA);
				}
				json.delete(json.length() - 1, json.length());
			}
			json.append(STR_OBJECT_RIGHT);
		} else {
			json.append(STR_OBJECT_LEFT);
			Field[] fields = getDeclaredFields(value.getClass());
			if (fields.length > 0) {
				for (Field field : fields) {
					Object fieldObj = getFieldObject(field, value);
					writeObjItem(field.getName(), fieldObj, json);
					json.append(STR_COMMA);
				}
				json.delete(json.length() - 1, json.length());
			}

			json.append(STR_OBJECT_RIGHT);
		}
	}

	/**
	 *
	 * @param clazz 类
	 * @return 返回类的字段名数组，并缓存
	 */
	public synchronized Field[] getDeclaredFields(Class<?> clazz) {
		String cacheKey = clazz.getName();
		if (cacheFields.containsKey(cacheKey)) {
			return cacheFields.get(cacheKey);
		}
		Field[] fields = getAllDeclaredFields(clazz);    //clazz.getDeclaredFields();
		cacheFields.put(cacheKey, fields);
		return fields;
	}

	/**
	 *
	 * @param clazz
	 * @return 通过反射返回类的字段名数组
	 */
	private Field[] getAllDeclaredFields(Class<?> clazz) {
		List<Field> list = new ArrayList<Field>();
		Class<?> current = clazz;

		while (current != null && current != Object.class) {
			Field[] fields = current.getDeclaredFields();

			for (Field field : fields) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				list.add(field);
			}

			current = current.getSuperclass();
		}

		return list.toArray(new Field[list.size()]);
	}

	/**
	 *
	 * @param field
	 * @param obj
	 * @return 反射获取字段对象
	 */
	private synchronized Object getFieldObject(Field field, Object obj) {
		try {
			field.setAccessible(true);
			return field.get(obj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error(e.getMessage(), e);
			return null;
		} finally {
			field.setAccessible(false);
		}
	}
}
