package com.github.qpcrummer.beatmaker.utils;

import imgui.type.ImDouble;

import java.util.Comparator;
import java.util.List;

public final class ListUtils {
    /**
     * Sorts a list in ascending order
     * @param intList List<Integer>
     */
    public static void sortInt(List<Integer> intList) {
        intList.sort(Comparator.comparingInt(a -> a));
    }

    /**
     * Sorts a list in ascending order
     * @param imDoubleList List<ImDouble[]>
     */
    public static void sortImDouble(List<ImDouble[]> imDoubleList) {
        imDoubleList.sort(Comparator.comparingDouble(a -> a[0].get()));
    }
}
