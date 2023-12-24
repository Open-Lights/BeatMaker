package com.github.qpcrummer.beatmaker.utils;

import imgui.type.ImDouble;

import java.util.Comparator;
import java.util.List;

public final class Comparer {
    /**
     * Compares two lists and removes overlap;
     * @param list1 First list to compare. This one always gets the interval removed!
     * @param list2 Second list to compare
     * @return A new list that is combining the two above
     */
    public static List<ImDouble[]> removeOverlap(List<ImDouble[]> list1, List<ImDouble[]> list2) {
        int list1LowIndex = 0;
        int list1HighIndex = 0;
        // Determine which is better
        List<ImDouble[]> bigger = List.copyOf(list1).get(0)[0].get() > list2.get(0)[0].get() ? list1 : list2;
        List<ImDouble[]> smaller = List.copyOf(list1).get(0)[0].get() < list2.get(0)[0].get() ? list1 : list2;

        // Find low index
        for (int i = 0; i < smaller.size(); i++) {
            if (smaller.get(i)[1].get() > bigger.get(0)[0].get()) {
                list1LowIndex = i;
                break;
            }
        }

        // Find high index
        for (int i = smaller.size() - 1; i >= 0; i--) {
            if (smaller.get(i)[0].get() < bigger.get(bigger.size() - 1)[1].get()) {
                list1HighIndex = i;
                break;
            }
        }

        // Mass deletion
        for (int i = list1HighIndex - list1LowIndex; i >= 0; i--) {
            smaller.remove(list1LowIndex);
        }

        smaller.addAll(bigger);
        ListUtils.sortImDouble(smaller);

        return smaller;
    }
}
