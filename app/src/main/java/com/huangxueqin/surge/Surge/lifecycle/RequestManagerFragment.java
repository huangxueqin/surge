package com.huangxueqin.surge.Surge.lifecycle;

import android.app.Activity;
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

    @Nullable private RequestManagerFragment rootFragment;
    @Nullable private RequestManager requestManager;

    public RequestManagerFragment() {
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public void setRequestManager(RequestManager manager) {
        requestManager = manager;
    }

    public void addChildRequestManagerFragment(RequestManagerFragment rmf) {
        childFragments.add(rmf);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        requestManager.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        requestManager.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requestManager.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
