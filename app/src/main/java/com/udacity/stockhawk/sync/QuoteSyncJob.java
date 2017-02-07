package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_SYNC = "com.udacity.stockhawk.ACTION_DATA_SYNC";
    public static final String ACTION_DATA_SYNC_FAILED = "com.udacity.stockhawk.ACTION_DATA_SYNC_FAILED";
    public static final String SYNC_EXTRA_STOCK_NAME = "com.udacity.stockhawk.SYNC_EXTRA_STOCK_NAME";
    private static final int PERIOD = 300000;
    private static final int STOCK_STALE_PERIOD = PERIOD * 3;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    public static final int SYNC_STATUS_DATA_UPDATED = 0;
    public static final int SYNC_STATUS_SERVER_DOWN = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SYNC_STATUS_DATA_UPDATED, SYNC_STATUS_SERVER_DOWN})
    public @interface SyncStatus {
    }

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {

        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();


                Stock stock = quotes.get(symbol);
                if (stock.getQuote() == null || stock.getQuote().getPrice() == null || stock.getQuote().getChangeInPercent() == null) {
                    Intent intent = new Intent(ACTION_DATA_SYNC_FAILED);
                    intent.putExtra(SYNC_EXTRA_STOCK_NAME, symbol);
                    context.sendBroadcast(intent);
                    continue;
                }
                StockQuote quote = stock.getQuote();

                String name = stock.getName();
                String stockExchange = stock.getStockExchange();

                float price = quote.getPrice().floatValue();
                float change = quote.getChange().floatValue();
                float percentChange = quote.getChangeInPercent().floatValue();

                // WARNING! Don't request historical data for a stock that doesn't exist!
                // The request will hang forever X_x
                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                StringBuilder historyBuilder = new StringBuilder();

                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    historyBuilder.append(", ");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append("\n");
                }

                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_NAME, name);
                quoteCV.put(Contract.Quote.COLUMN_STOCK_EXCHANGE, stockExchange);
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);


                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                quoteCVs.add(quoteCV);

            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_SYNC);
            context.sendBroadcast(dataUpdatedIntent);
            setSyncStatus(context, SYNC_STATUS_DATA_UPDATED);

        } catch (IOException exception) {
            setSyncStatus(context, SYNC_STATUS_SERVER_DOWN);
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());

        }
    }

    private static void setSyncStatus(Context pContext, @SyncStatus int pSyncStatus) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(pContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(pContext.getString(R.string.sync_status_key), pSyncStatus);
        if (pSyncStatus == SYNC_STATUS_DATA_UPDATED)
            editor.putLong(pContext.getString(R.string.sync_timestamp), System.currentTimeMillis());
        editor.commit();
    }

    public static boolean isStockOutDated(Context pContext) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(pContext);
        long currentTimeStamp = System.currentTimeMillis();
        long syncTimeStamp = preferences.getLong(pContext.getString(R.string.sync_timestamp), currentTimeStamp);
        return currentTimeStamp - syncTimeStamp > STOCK_STALE_PERIOD;
    }

    public static int getSyncStatus(Context pContext) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(pContext);
        return preferences.getInt(pContext.getString(R.string.sync_status_key), SYNC_STATUS_DATA_UPDATED);
    }
}
