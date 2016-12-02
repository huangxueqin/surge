package com.huangxueqin.surge.Surge.request;

/**
 * Created by huangxueqin on 2016/11/30.
 */

public interface Request {
    int MSG_REQUEST_SUCCESS = 0x100;
    int MSG_REQUEST_FAIL = 0x101;

    void start();
    void resume();
    void suspend();
    void cancel();
}
