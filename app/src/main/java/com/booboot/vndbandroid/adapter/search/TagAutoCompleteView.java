package com.booboot.vndbandroid.adapter.search;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.booboot.vndbandroid.R;
import com.booboot.vndbandroid.model.vndb.Tag;
import com.booboot.vndbandroid.util.Utils;
import com.tokenautocomplete.TokenCompleteTextView;

/**
 * Created by od on 20/06/2016.
 */
public class TagAutoCompleteView extends TokenCompleteTextView<Tag> {
    public TagAutoCompleteView(Context context) {
        super(context);
    }

    public TagAutoCompleteView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TagAutoCompleteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View getViewForObject(Tag tag) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        LinearLayout view = (LinearLayout) l.inflate(R.layout.tag_token, (ViewGroup) TagAutoCompleteView.this.getParent(), false);

        Drawable background = view.getBackground();
        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable)background).getPaint().setColor(Utils.getThemeColor(getContext(), R.attr.colorAccent));
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable)background).setColor(Utils.getThemeColor(getContext(), R.attr.colorAccent));
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable)background).setColor(Utils.getThemeColor(getContext(), R.attr.colorAccent));
        }

        ((TextView) view.findViewById(R.id.tagTokenName)).setText(tag.getName());

        return view;
    }

    @Override
    /**
     * Return a default tag when a delimiter is typed (, ;) or when the current word being typed is cut by the end of the line.
     * In the latter case, we don't want our word being automatically transformed into a tag when we reach the end of a line.
     * We just want to keep typing our word. That's why we return null here (we don't need a default object anyway).
     */
    protected Tag defaultObject(String completionText) {
        return null;
    }
}