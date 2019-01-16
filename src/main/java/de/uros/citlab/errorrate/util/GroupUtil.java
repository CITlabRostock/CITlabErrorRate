/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author gundram
 */
public class GroupUtil {

    public static <T, D> List<D> getGrouping(List<T> list, Joiner<T> joiner, Mapper<T, D> mapper) {
        List<List<T>> grouping = getGrouping(list, joiner);
        List<D> res = new ArrayList<>(grouping.size());
        for (List<T> list1 : grouping) {
            res.add(mapper.map(list1));
        }
        return res;
    }

    public static <T> List<List<T>> getGrouping(List<T> list, Joiner<T> joiner) {
        LinkedList<List<T>> res = new LinkedList<>();
        LinkedList<T> cache = new LinkedList<>();
        for (T element : list) {
            if (!cache.isEmpty()) {
                if (!joiner.isGroup(cache, element)) {
                    res.add(cache);
                    cache = new LinkedList<>();
                }
            }
            if (joiner.keepElement(element)) {
                cache.add(element);
            }
        }
        if (!cache.isEmpty()) {
            res.add(cache);
        }
        return res;
    }

    public static interface Joiner<T> {

        public boolean isGroup(List<T> group, T element);

        public boolean keepElement(T element);
    }

    public static interface Mapper<T, D> {

        public D map(List<T> elements);
    }
}
