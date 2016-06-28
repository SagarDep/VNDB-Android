package com.booboot.vndbandroid.factory;

import com.booboot.vndbandroid.adapter.doublelist.DoubleListElement;
import com.booboot.vndbandroid.api.bean.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by od on 28/06/2016.
 */
public class TagDataFactory {
    public static DoubleListElement[] getData(Tag tag) {
        final List<DoubleListElement> tagData = new ArrayList<>();
        if (tag.getDescription() != null) {
            tagData.add(new DoubleListElement("Description", tag.getDescription(), true));
        }

        return tagData.toArray(new DoubleListElement[tagData.size()]);
    }
}
