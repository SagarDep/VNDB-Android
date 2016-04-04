package com.booboot.vndbandroid.adapter.vndetails;

/**
 * Created by od on 18/03/2016.
 */

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.booboot.vndbandroid.R;
import com.booboot.vndbandroid.util.Lightbox;
import com.booboot.vndbandroid.util.Pixels;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.LinkedHashMap;
import java.util.List;

public class VNExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> expandableListTitle;
    private LinkedHashMap<String, VNDetailsElement> expandableListDetail;

    public VNExpandableListAdapter(Context context, List<String> expandableListTitle, LinkedHashMap<String, VNDetailsElement> expandableListDetail) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return expandableListDetail.get(expandableListTitle.get(listPosition)).getLeftData().get(expandedListPosition);
    }

    public List<Integer> getLeftImages(int listPosition) {
        return expandableListDetail.get(expandableListTitle.get(listPosition)).getLeftImages();
    }

    public List<Integer> getRightImages(int listPosition) {
        return expandableListDetail.get(expandableListTitle.get(listPosition)).getRightImages();
    }

    public Object getRightChild(int listPosition, int expandedListPosition) {
        List<String> rightData = expandableListDetail.get(expandableListTitle.get(listPosition)).getRightData();
        if (rightData == null || rightData.size() <= expandedListPosition) return null;
        return rightData.get(expandedListPosition);
    }

    public int getChildLayout(int listPosition) {
        int type = expandableListDetail.get(expandableListTitle.get(listPosition)).getType();
        if (type == VNDetailsElement.TYPE_TEXT) return R.layout.list_item_text;
        if (type == VNDetailsElement.TYPE_IMAGES) return R.layout.list_item_images;
        return -1;
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final String leftText = (String) getChild(listPosition, expandedListPosition);
        final String rightText = (String) getRightChild(listPosition, expandedListPosition);
        final int layout = getChildLayout(listPosition);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = layoutInflater.inflate(layout, null);

        switch (layout) {
            case R.layout.list_item_text:
                ImageButton itemLeftImage = (ImageButton) convertView.findViewById(R.id.itemLeftImage);
                TextView itemLeftText = (TextView) convertView.findViewById(R.id.itemLeftText);
                TextView itemRightText = (TextView) convertView.findViewById(R.id.itemRightText);
                ImageButton itemRightImage = (ImageButton) convertView.findViewById(R.id.itemRightImage);

                itemLeftText.setText(Html.fromHtml(leftText));

                if (rightText == null) itemRightText.setVisibility(View.GONE);
                else {
                    itemRightText.setMovementMethod(LinkMovementMethod.getInstance());
                    itemRightText.setText(Html.fromHtml(rightText));
                }

                int leftImage;
                if (getLeftImages(listPosition) != null && (leftImage = getLeftImages(listPosition).get(expandedListPosition)) > 0) {
                    itemLeftImage.setImageResource(leftImage);
                } else {
                    itemLeftImage.setVisibility(View.GONE);
                }

                int rightImage;
                if (getRightImages(listPosition) != null && (rightImage = getRightImages(listPosition).get(expandedListPosition)) > 0) {
                    itemRightImage.setImageResource(rightImage);
                } else {
                    itemRightImage.setVisibility(View.GONE);
                }
                break;

            case R.layout.list_item_images:
                final ImageButton expandedListImage = (ImageButton) convertView.findViewById(R.id.expandedListImage);
                ImageLoader.getInstance().displayImage(leftText, expandedListImage);
                convertView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Pixels.px(100, context)));
                Lightbox.set(context, expandedListImage, leftText);
                break;
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        return expandableListDetail.get(expandableListTitle.get(listPosition)).getLeftData().size();
    }

    @Override
    public Object getGroup(int listPosition) {
        return expandableListTitle.get(listPosition);
    }

    @Override
    public int getGroupCount() {
        return expandableListTitle.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String listTitle = (String) getGroup(listPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_group, null);
        }
        TextView listTitleTextView = (TextView) convertView.findViewById(R.id.listTitle);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        listTitleTextView.setText(listTitle);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }
}