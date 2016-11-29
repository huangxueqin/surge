package com.huangxueqin.surge.Surge.lifecycle;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.huangxueqin.surge.Surge.RequestManager;
import com.huangxueqin.surge.Surge.Utils.Singleton;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by huangxueqin on 2016/11/29.
 */

public class RequestManagerRetriever implements Handler.Callback {
    private static final String TAG = "rmfragment";
    private static final int MSG_REMOVE_PENDING_FRAGMENT = 1;
    private static final Singleton<RequestManagerRetriever> sDefault = new Singleton<RequestManagerRetriever>() {
        @Override
        protected RequestManagerRetriever create() {
            return new RequestManagerRetriever();
        }
    };
    private HashMap<FragmentManager, RequestManagerFragment> pendingFragments = new HashMap<>();
    private Handler handler;

    private RequestManagerRetriever() {
        handler = new Handler(Looper.getMainLooper(), this);
    }

    public static RequestManagerRetriever getDefault() {
        return sDefault.get();
    }

    public RequestManager get(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            throw new IllegalStateException("activity is already destroyed");
        }
        FragmentManager fm = activity.getFragmentManager();
        return get(activity, fm);
    }

    public RequestManager get(Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalStateException("fragment must attach to an activity before load image");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            FragmentManager fm = fragment.getChildFragmentManager();
            return get(fragment.getActivity(), fm);
        } else {
            return get(fragment.getActivity());
        }
    }

    private RequestManager get(Context context, FragmentManager fm) {

    }

    private RequestManagerFragment getRequestManagerFragment(FragmentManager fm) {
        RequestManagerFragment rmFragment = (RequestManagerFragment) fm.findFragmentByTag(TAG);
        if (rmFragment == null) {
            rmFragment = pendingFragments.get(fm);
        }
        if (rmFragment == null) {
            rmFragment = new RequestManagerFragment();
            pendingFragments.put(fm, rmFragment);
            fm.beginTransaction().add(rmFragment, TAG).commitAllowingStateLoss();
            handler.obtainMessage(MSG_REMOVE_PENDING_FRAGMENT, fm).sendToTarget();
        }
        return rmFragment;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_REMOVE_PENDING_FRAGMENT) {
            FragmentManager fm = (FragmentManager) msg.obj;
            pendingFragments.remove(fm);
            return true;
        }
        return false;
    }
}
