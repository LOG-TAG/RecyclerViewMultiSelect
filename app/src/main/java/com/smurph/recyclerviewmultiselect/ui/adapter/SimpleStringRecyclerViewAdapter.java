package com.smurph.recyclerviewmultiselect.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.smurph.multiselectlib.MultiSelectAdapter;
import com.smurph.multiselectlib.MultiSelectHelper;
import com.smurph.multiselectlib.MultiSelectViewHolder;
import com.smurph.recyclerviewmultiselect.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ben on 8/30/15.
 *
 */
public class SimpleStringRecyclerViewAdapter
        extends MultiSelectAdapter<SimpleStringRecyclerViewAdapter.ViewHolder> {

    private int mBackground;
    private List<String> mValues = new ArrayList<>();
    private MultiSelectHelper mHelper;
    private final Object mLock = new Object();

    public interface AdapterListener {
        void onCABMenuStart();
        void onCABMenuFinish();
        void updateTitleCount(int count);
    }
    private AdapterListener mAdapterListener;

    @Retention(RetentionPolicy.CLASS)
    @IntDef({HELPER_MULTI_WITH_CAB, HELPER_SINGLE_WITH_CAB,
            HELPER_MULTI_WITHOUT_CAB, HELPER_SINGLE_WITHOUT_CAB,
            HELPER_VIEWPAGER_CAB})
    public @interface HelperTypeDef{}
    public static final int HELPER_MULTI_WITH_CAB = 1;
    public static final int HELPER_SINGLE_WITH_CAB = 2;
    public static final int HELPER_MULTI_WITHOUT_CAB = 3;
    public static final int HELPER_SINGLE_WITHOUT_CAB = 4;
    public static final int HELPER_VIEWPAGER_CAB = 5;

    public SimpleStringRecyclerViewAdapter(@NonNull Context context, @Nullable List<String> items,
                                           @HelperTypeDef int type) {
        super(context);
        switch (type) {
            case HELPER_MULTI_WITH_CAB:
                //NOTE  Helper with Contextual Action Mode (CAB)
                mHelper = new MultiSelectHelper(context);
                mHelper.setActionModeCallback(mActionModeCallback);
                break;
            case HELPER_SINGLE_WITH_CAB:
                // NOTE Helper with CAB but only selects one item at a time
                mHelper = new MultiSelectHelper(context)
                        .setSingleSelectMode(true);
                mHelper.setActionModeCallback(mActionModeCallback);
                break;
            case HELPER_SINGLE_WITHOUT_CAB:
                //NOTE Helper that has no CAB and will only select one item at a time
                mHelper = new MultiSelectHelper(context)
                    .setActionModeEnabled(false)
                    .setSingleSelectMode(true);
                break;
            case HELPER_MULTI_WITHOUT_CAB:
            // NOTE Helper that has no CAB but still can select multiple positions
                mHelper = new MultiSelectHelper(context)
                        .setActionModeEnabled(false);
                break;
            case HELPER_VIEWPAGER_CAB:
                mHelper = new MultiSelectHelper(context);
                mHelper.setActionModeCallback(mActionModeCallback);
                break;
            default: break;
        }
        //NOTE: Examples to show setting selected color at runtime.
//            mHelper.setSelectedColor(Color.CYAN);
//            mHelper.setSelectedColor(MainActivity.this, R.color.theme_cyan);
        setMultiSelectHelper(mHelper);

        TypedValue mTypedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
        mBackground = mTypedValue.resourceId;
        if (items!=null) { addAll(items, false); }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        v.setBackgroundResource(mBackground);
        ViewHolder vH = new ViewHolder(v);
        attachViewHolder(vH);
        return vH;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // NOTE: This must be called.
        super.onBindViewHolder(holder, position);
        if (mHelper.getIsSelected(position)) {
            mHelper.setRippleColor(holder.itemView);
            holder.mTxtView.setTextColor(Color.WHITE);
        } else {
            holder.itemView.setBackgroundResource(mBackground);
            holder.mTxtView.setTextColor(Color.BLACK);
        }
        holder.mString = mValues.get(position);
        holder.mTxtView.setText(mValues.get(position));
    }

    @Override
    public int getItemCount() { return mValues.size(); }

    public void addAll(@NonNull Collection<? extends String> items) { addAll(items, true); }

    private void addAll(@NonNull Collection<? extends String> items, boolean notifyChange) {
        synchronized (mLock) { mValues.addAll(items); }
        if (notifyChange) { notifyDataSetChanged(); }
    }

    public void add(@NonNull String item, boolean notifyChange) {
        synchronized (mLock) { mValues.add(item); }
        int position = mValues.indexOf(item);
        if (notifyChange) { notifyItemInserted(position); }
    }

    public void insert(@NonNull String item, int position, boolean notifyChange) {
        synchronized (mLock) { mValues.add(position, item); }
        if (notifyChange) { notifyItemInserted(position); }
    }

    public void remove(String item, boolean notifyChange) {
        int position = mValues.indexOf(item);
        synchronized (mLock) { mValues.remove(item); }
        if (notifyChange) { notifyItemRemoved(position); }
    }

    public void clear(boolean notifyChange) {
        synchronized (mLock) { mValues.clear(); }
        if (notifyChange) { notifyDataSetChanged(); }
    }

    public class ViewHolder extends MultiSelectViewHolder {
        public String mString;
        public TextView mTxtView;

        public ViewHolder(View v) {
            super(v);
            mTxtView = (TextView) v.findViewById(android.R.id.text1);
        }
    }

    public void setIsClickingEnabled(boolean isEnabled) { mHelper.setIsClickingEnabled(isEnabled); }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mAdapterListener!=null) { mAdapterListener.onCABMenuStart(); }
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.cab_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (getContext()==null) { return false; }

            mode.setTitle(getContext().getResources()
                    .getQuantityString(R.plurals.selected_items,
                            getSelectedCount(), getSelectedCount()));
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    if (getContext()!=null) {
                        Toast.makeText(getContext(), "Delete these.", Toast.LENGTH_SHORT).show();
                    }
                    List<Integer> list = mHelper.getSelectedPositions();
                    List<String> items = new ArrayList<>(list.size());
                    for (Integer i : list) items.add(mValues.get(i)); ;
                    list.clear();
                    list = null;
                    for (String s : items) { remove(s, false); }
                    notifyDataSetChanged();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (mAdapterListener!=null) { mAdapterListener.onCABMenuFinish(); }
            mHelper.destroyActionMode();
        }
    };

    public void setAdapterListener(AdapterListener l) { mAdapterListener = l; }
}
