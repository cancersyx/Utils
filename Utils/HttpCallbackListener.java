package com.zsf.demotest;

/**
 * Created by zsf on 2017/2/7.
 */

public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
