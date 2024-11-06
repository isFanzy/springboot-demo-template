package top.fanzy.template.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TreeUtils {
    /**
     * @description: 树形结构构建
     * @param: [list, rootCheck, parentCheck, setSubChildren]
     * @types: List<E>,Predicate<E>,BiFunction<E,E,Boolean>,BiConsumer<E,List<E>>
     * @return: java.util.List<E>
     * @date: 2024/9/11 10:16
     */
    public static <E> List<E> makeTree(List<E> list, Predicate<E> rootCheck, BiFunction<E, E, Boolean> parentCheck, BiConsumer<E, List<E>> setSubChildren) {
        return list.stream().filter(rootCheck).peek(x -> setSubChildren.accept(x, makeChildren(x, list, parentCheck, setSubChildren))).collect(Collectors.toList());
    }

    private static <E> List<E> makeChildren(E parent, List<E> allData, BiFunction<E, E, Boolean> parentCheck, BiConsumer<E, List<E>> setSubChildren) {
        return allData.stream().filter(x -> parentCheck.apply(parent, x)).peek(x -> setSubChildren.accept(x, makeChildren(x, allData, parentCheck, setSubChildren))).collect(Collectors.toList());
    }

    /**
     * @description: 差集计算(list1中有，list2中没有的值或对象)
     * @param: [list1, list2, key]
     * @types: List<T>,List<U>,String
     * @return: java.util.List<T>
     * @date: 2024/10/11 10:25
     */
    public static <T, U, K> List<T> queryDifferentSet(List<T> list1, List<U> list2, String key, Class<K> type) {
        // 基本类型直接取值
        List<T> res = new ArrayList<>();
        if (key == null) {
            res = list1;
            res.removeAll(list2);
        } else {
            res = list1.stream().filter(obj1 -> {
                try {
                    K list1value = getFieldValue(obj1, key, type);

                    return list2.stream()
                            .map(obj2 -> getFieldValue(obj2, key, type))
                            .noneMatch(value -> value != null && value.equals(list1value));
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }).collect(Collectors.toList());
        }

        return res;
    }

	/**
     * @description: 交集计算
     * @param: [list1, list2, key]
     * @types: List<T>,List<U>,String
     * @return: java.util.List<T>
     * @date: 2024/10/11 10:25
     */
    public static <T, U, K> List<T> queryIntersectionSet(List<T> list1, List<U> list2, String key, Class<K> type) {
        List<T> res = new ArrayList<>();
        if (key == null) {
            res.addAll(list1);
            res.retainAll(list2);
        } else {
            res = list1.stream().filter(obj1 -> {
                try {
                    K list1value = getFieldValue(obj1, key, type);
                    return list2.stream()
                            .map(obj2 -> getFieldValue(obj2, key, type))
                            .anyMatch(value -> value != null && value.equals(list1value));
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }).collect(Collectors.toList());
        }

        return res;
    }

    private static <K> K getFieldValue(Object obj, String key, Class<K> type) {
        if (obj instanceof Map) {
            return type.cast(((Map<String, Object>) obj).get(key));
        } else if (obj instanceof JSONObject) {
            return type.cast(JSONObject.parseObject(JSON.toJSONString(obj)).get(key));
        } else {
            try {
                Field field = obj.getClass().getDeclaredField(key);
                field.setAccessible(true);
                Object value = field.get(obj);
                return type.cast(value != null ? value : null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                try {
                    Field field = obj.getClass().getSuperclass().getDeclaredField(key);
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    return type.cast(value != null ? value : null);
                } catch (NoSuchFieldException | IllegalAccessException e2) {
                    e2.printStackTrace();
                    return null;
                }
            } catch (ClassCastException e) {
                e.printStackTrace();
                return null;
            }
        }

    }
}
