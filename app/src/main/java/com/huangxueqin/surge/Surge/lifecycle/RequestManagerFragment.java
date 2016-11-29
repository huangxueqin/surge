package com.huangxueqin.surge.Surge.lifecycle;

import android.app.Fragment;
import android.content.Context;
import android.support.annotation.Nullable;

import com.huangxueqin.surge.Surge.RequestManager;

import java.util.HashSet;

/**
 * Created by huangxueqin on 2016/11/29.
 */

public class RequestManagerFragment extends Fragment {

    private HashSet<RequestManagerFragment> childFragments = new HashSet<>();

    @Nullable private RequestManagerFragment parentFragment;
    @Nullable private RequestManager requestManager;

    public RequestManagerFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
