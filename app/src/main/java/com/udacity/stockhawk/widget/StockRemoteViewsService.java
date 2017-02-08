package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.DetailsActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by Fabin Paul, Eous Solutions Delivery on 2/7/2017 10:27 AM.
 */

public class StockRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor mData;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (mData != null)
                    mData.close();

                final long identityToken = Binder.clearCallingIdentity();
                mData = getContentResolver().query(Contract.Quote.URI,
                        Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                        null, null, Contract.Quote.COLUMN_SYMBOL);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (mData != null) {
                    mData.close();
                    mData = null;
                }
            }

            @Override
            public int getCount() {
                return mData == null ? 0 : mData.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION || mData == null || !mData.moveToPosition(position)) {
                    return null;
                }
                RemoteViews view = new RemoteViews(getPackageName(), R.layout.widget_list_item_stock);
                DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                DecimalFormat percentFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.US);
                percentFormat.setMaximumFractionDigits(2);
                percentFormat.setMinimumFractionDigits(2);
                percentFormat.setPositivePrefix("+");
                String symbol = mData.getString(Contract.Quote.POSITION_SYMBOL);
                view.setTextViewText(R.id.symbol, symbol);
                String stockExchange = mData.getString(Contract.Quote.POSITION_STOCK_EXCHANGE);
                float price = mData.getFloat(Contract.Quote.POSITION_PRICE);
                view.setTextViewText(R.id.price, dollarFormat.format(price));
                dollarFormat.setPositivePrefix("+$");

                float rawAbsoluteChange = mData.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = mData.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                if (rawAbsoluteChange > 0) {
                    view.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else {
                    view.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }

                String change = dollarFormat.format(rawAbsoluteChange);
                String percentage = percentFormat.format(percentageChange / 100);

                if (PrefUtils.getDisplayMode(StockRemoteViewsService.this)
                        .equals(getString(R.string.pref_display_mode_absolute_key))) {
                    view.setTextViewText(R.id.change, change);
                } else {
                    view.setTextViewText(R.id.change, percentage);
                }

                final Intent fillInIntent = new Intent();
                final Bundle bundle = new Bundle();
                bundle.putString(DetailsActivity.INTENT_EXTRA_SYMBOL,
                        symbol);
                bundle.putString(DetailsActivity.INTENT_EXTRA_STOCK_EXCHANGE,
                        stockExchange);
                fillInIntent.putExtras(bundle);
                view.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return view;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item_stock);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (mData.moveToPosition(position))
                    return mData.getLong(Contract.Quote.POSITION_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
