package com.udacity.stockhawk.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.udacity.stockhawk.R;

public class DetailsActivity extends AppCompatActivity {

    public static final String INTENT_EXTRA_SYMBOL = "com.udacity.stockhawk.Extra_Symbol";
    public static final String INTENT_EXTRA_STOCK_EXCHANGE = "com.udacity.stockhawk.Extra_Stock_Exchange";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        String symbol = getIntent().getStringExtra(INTENT_EXTRA_SYMBOL);
        String stockExchange = getIntent().getStringExtra(INTENT_EXTRA_STOCK_EXCHANGE);

        ((TextView) findViewById(R.id.toolbar_title)).setText(symbol + " (" + stockExchange + ")");

        if (null == savedInstanceState && !TextUtils.isEmpty(symbol)) {
            DetailsFragment fragment = DetailsFragment.newInstance(symbol);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.detail_frag_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void startActivity(Activity fromActivity, String symbol, String stockExchange, View transitionView) {
        Intent intent = new Intent(fromActivity, DetailsActivity.class);
        intent.putExtra(INTENT_EXTRA_SYMBOL, symbol);
        intent.putExtra(INTENT_EXTRA_STOCK_EXCHANGE, stockExchange);
//        fromActivity.startActivity(intent);
        ActivityOptionsCompat activityOptions =
                ActivityOptionsCompat.makeSceneTransitionAnimation(fromActivity,
                        new Pair<View, String>(transitionView, fromActivity.getString(R.string.stock_symbol_transistion_name)));
        ActivityCompat.startActivity(fromActivity, intent, activityOptions.toBundle());
    }
}
