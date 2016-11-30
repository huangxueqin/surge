package com.huangxueqin.surge;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.huangxueqin.surge.Surge.Surge;

public class MainActivity extends AppCompatActivity {

    ListView mList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mList = (ListView) findViewById(R.id.list);
        mList.setAdapter(new MyAdapter(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    static class MyAdapter extends BaseAdapter {
        Context mContext;

        public MyAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return 10;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            }
            ImageView image = (ImageView) convertView.findViewById(R.id.image);
            image.getContext();
            Surge.with(mContext).loadImage(images[position], image);
            return convertView;
        }
    }

    static String[] images = {
            "http://static.cnbetacdn.com/article/2016/1113/bf50b1cbdde81a5.jpg",
            "http://img.hb.aicdn.com/2369fc55012ad26c6b5a853db7b45c744804b929425d3-iZ2TiO_fw658",
            "http://img.hb.aicdn.com/1b4c9539fc187b7c9bd7f2c1553efa4265f26b963f9b8-u0VKDC_fw658",
            "http://img.hb.aicdn.com/8979155e3ed407c8d66f09fb42265d3653342f3efd768-1jdsx4_fw658",
            "http://img.hb.aicdn.com/ee306fab2bdf40dfcf0b9ec446891b4f0e50643634255-IsVoSb_fw658",
            "http://img.hb.aicdn.com/ff96038974199eec737677e19924c467109584875a85e-tgNk5F_fw658",
            "http://img.hb.aicdn.com/26e0a89ee5c5818c10e41e98592faf9e2566443614fe3-DJghBb_fw658",
            "http://img.hb.aicdn.com/b56724b791ac246a01f7546f0f15a0532d20759dc64-0qHNII_fw658",
            "http://img.hb.aicdn.com/d763e381e40c1c6b7d83dd57ec0b3feb89b34d903be54-nILlJB_fw658",
            "http://img.hb.aicdn.com/09c545a6dc551a442420ead56a58bbfae13ef19b3a37f-pXNiic_fw658",
            "http://img.hb.aicdn.com/078c629f3fec9382dc7a1cf60884486abdd2c6204e9c-KC738P_fw658"
    };
}
