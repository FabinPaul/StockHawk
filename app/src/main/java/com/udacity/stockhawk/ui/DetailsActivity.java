package com.udacity.stockhawk.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.udacity.stockhawk.R;

public class DetailsActivity extends AppCompatActivity {

    public static final String INTENT_EXTRA_SYMBOL = "com.udacity.stockhawk.Extra_Symbol";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String symbol = getIntent().getStringExtra(INTENT_EXTRA_SYMBOL);

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

    public static void startActivity(Activity fromActivity, String symbol) {
        Intent intent = new Intent(fromActivity, DetailsActivity.class);
        intent.putExtra(INTENT_EXTRA_SYMBOL, symbol);
        fromActivity.startActivity(intent);
    }
}
