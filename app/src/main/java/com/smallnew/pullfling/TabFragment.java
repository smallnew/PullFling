package com.smallnew.pullfling;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.smallnew.pullflinglib.GoListView;


public class TabFragment extends Fragment {
    public static final String TITLE = "title";
    private String mTitle = "Defaut Value";
    private TextView mTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(TITLE);
            DecelerateInterpolator dd;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mTitle.equals("333")) {
            GoListView view = (GoListView) inflater.inflate(R.layout.fragment_tab_list, container, false);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
            for (int i = 0; i < 20; i++) {
                adapter.add("item " + String.valueOf(i));
            }
            view.setAdapter(adapter);
            return view;
        }
        View view = inflater.inflate(R.layout.fragment_tab, container, false);
        mTextView = (TextView) view.findViewById(R.id.id_info);
        mTextView.setText(mTitle);
        return view;

    }

    public static TabFragment newInstance(String title) {
        TabFragment tabFragment = new TabFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);
        tabFragment.setArguments(bundle);
        return tabFragment;
    }

}
