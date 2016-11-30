package com.huangxueqin.surge.Surge.lifecycle;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.huangxueqin.surge.Surge.RequestManager;

/**
 * Created by huangxueqin on 2016/11/30.
 */

public class SupportRequestManagerFragment extends Fragment {
    @Nullable private RequestManager requestManager;

    public SupportRequestManagerFragment() {

    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public void setRequestManager(RequestManager manager) {
        requestManager = manager;
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
