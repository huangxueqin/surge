package com.huangxueqin.surge.Surge.request;


import com.huangxueqin.surge.Surge.Utils.Size;

/**
 * Created by huangxueqin on 2016/11/30.
 * RequestToken are used to identify a network request
 */

abstract public class RequestToken implements Request {
    String url;
    Size size;

    public RequestToken(String url, Size size) {
        this.url = url;
        this.size = new Size(size.width, size.height);
    }

    public RequestToken(RequestToken token) {
        this(token.url, token.size);
    }

    public RequestToken(String url) {
        this(url, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof RequestToken) {
            RequestToken t = (RequestToken) o;
            return t.url.equals(url) && equalSize(t.size, size);
        }
        return false;
    }

    private static boolean equalSize(Size lhs, Size rhs) {
        if (lhs == null) {
            return rhs == null;
        }
        return lhs.equals(rhs);
    }
}
