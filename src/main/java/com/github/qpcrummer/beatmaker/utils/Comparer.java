package com.github.qpcrummer.beatmaker.utils;

import imgui.type.ImDouble;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class Comparer {

    /**
     * Removes overlap from the later Chart
     * @param list1 Chart 1's timestamps
     * @param list2 Chart 2's timestamps
     * @return Combined list of both charts with removed overlap
     */
    public static List<ImDouble[]> removeOverlap(List<ImDouble[]> list1, List<ImDouble[]> list2) {
        double listOneStartVal = list1.getFirst()[0].get();
        double listTwoStartVal = list2.getFirst()[0].get();

        if (listOneStartVal < listTwoStartVal) {
            double listOneEndVal = list1.getLast()[1].get();
            if (listOneEndVal == 0) {
                listOneEndVal = list1.getLast()[0].get();
            }

            ListIterator<ImDouble[]> iterator = list2.listIterator();
            while (iterator.hasNext()) {
                ImDouble[] timestamp = iterator.next();
                if (timestamp[0].get() < listOneEndVal) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        } else {
            double listTwoEndVal = list2.getLast()[1].get();
            if (listTwoEndVal == 0) {
                listTwoEndVal = list2.getLast()[0].get();
            }

            ListIterator<ImDouble[]> iterator = list1.listIterator();
            while (iterator.hasNext()) {
                ImDouble[] timestamp = iterator.next();
                if (timestamp[0].get() < listTwoEndVal) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        }

        List<ImDouble[]> newList = new ArrayList<>(list1);
        newList.addAll(list2);

        ListUtils.sortImDouble(newList);
        return newList;
    }
}
