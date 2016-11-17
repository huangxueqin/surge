package com.huangxueqin.surge.Surge;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class SurgeDownloader implements Callable<InputStream> {
    private final static int READ_TIMEOUT = 5000;
    private final static int CONNECT_TIMEOUT = 5000;

    String remoteURL;
    boolean cancelled;

    public SurgeDownloader(String url) {
        remoteURL = url;
    }

    @Override
    public InputStream call() {
        if (cancelled) {
            return null;
        }
        try {
            URL url = new URL(remoteURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (cancelled) {
                    return null;
                }
                return conn.getInputStream();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
