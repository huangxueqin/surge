package com.huangxueqin.surge.Surge.request;

/**
 * Created by huangxueqin on 2016/11/30.
 */

public interface Request {
    void start();
    void resume();
    void suspend();
    void cancel();
}
