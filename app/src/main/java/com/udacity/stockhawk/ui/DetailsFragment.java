package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String BUNDLE_EXTRA_SYMBOL = "com.udacity.stockhawk.BUNDLE_EXTRA_SYMBOL";
    private static final int STOCK_LOADER = 1;
    private static final String TAG = DetailsFragment.class.getSimpleName();
    private Unbinder mUnbinder;

    @BindView(R.id.stock_graph)
    LineChart mGraphView;

    @BindView(R.id.stock_name)
    TextView mStockName;
    @BindView(R.id.absolute_change)
    TextView mAbsoluteChange;
    @BindView(R.id.percentage_change)
    TextView mPercentageChange;
    @BindView(R.id.current_price)
    TextView mCurrentPrice;
    @BindView(R.id.stock_exchange)
    TextView mStockExchange;

    public static DetailsFragment newInstance(String pSymbol) {
        DetailsFragment fragment = new DetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_EXTRA_SYMBOL, pSymbol);
        fragment.setArguments(bundle);
        return fragment;
    }

    public DetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        initGraph();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String symbol = null;
        if (savedInstanceState != null) {
            symbol = getBundleExtraSymbol(savedInstanceState);
            initLoader(savedInstanceState);
        } else if (getArguments() != null) {
            symbol = getBundleExtraSymbol(getArguments());
            initLoader(getArguments());
        }

        if (!TextUtils.isEmpty(symbol) && null != getActivity().getActionBar()) {
            getActivity().getActionBar().setTitle(symbol);
        }
    }

    public void initLoader(Bundle pBundle) {
        getLoaderManager().initLoader(STOCK_LOADER, pBundle, this);
    }

    public String getBundleExtraSymbol(Bundle pBundle) {
        return pBundle.getString(BUNDLE_EXTRA_SYMBOL);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String symbol = getBundleExtraSymbol(args);
        return new CursorLoader(getActivity(),
                Contract.Quote.makeUriForStock(symbol),
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                Contract.Quote.COLUMN_SYMBOL + " = ?",
                new String[]{symbol},
                null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            String historyItems = data.getString(Contract.Quote.POSITION_HISTORY);
            mStockName.setText(data.getString(Contract.Quote.POSITION_NAME));
            DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            float rawAbsoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            if (rawAbsoluteChange > 0) {
                mAbsoluteChange.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                mPercentageChange.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                mAbsoluteChange.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                mPercentageChange.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }

            mCurrentPrice.setText(dollarFormat.format(data.getFloat(Contract.Quote.POSITION_PRICE)));
            dollarFormat.setPositivePrefix("+$");
            DecimalFormat percentFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.US);
            percentFormat.setMinimumFractionDigits(2);
            percentFormat.setMaximumIntegerDigits(2);
            percentFormat.setPositivePrefix("+");

            String absChangeString = null;
            String percentChangeString = null;
            if (rawAbsoluteChange > 0) {
                absChangeString = getString(R.string.up_arrow_value, dollarFormat.format(rawAbsoluteChange));
                percentChangeString = getString(R.string.up_arrow_value, percentFormat.format(percentageChange / 100));
            } else {
                absChangeString = getString(R.string.down_arrow_value, dollarFormat.format(rawAbsoluteChange));
                percentChangeString = getString(R.string.down_arrow_value, percentFormat.format(percentageChange / 100));
            }

            mAbsoluteChange.setText(absChangeString);
            mPercentageChange.setText(percentChangeString);

            mAbsoluteChange.setContentDescription(getString(R.string.stock_change_value, absChangeString));
            mPercentageChange.setContentDescription(getString(R.string.stock_change_value, percentChangeString));

            mStockExchange.setText(data.getString(Contract.Quote.POSITION_STOCK_EXCHANGE));
            String[] listOfHistoryItems = historyItems.split("\\n");
            Log.d(TAG, historyItems);
            loadGraph(listOfHistoryItems);
        }
    }

    private void loadGraph(String[] listOfHistoryItems) {
        ArrayList<Entry> dataPoints = new ArrayList<Entry>();

        for (int i = 0; i < listOfHistoryItems.length; i++) {
            String partsOfItem[] = listOfHistoryItems[i].split(", ");
            long timeStamp = Long.parseLong(partsOfItem[0]);
            float value = Float.parseFloat(partsOfItem[1]);
            dataPoints.add(new Entry(timeStamp, value));
        }
        Collections.sort(dataPoints, new EntryXComparator());
        LineDataSet dataSet = new LineDataSet(dataPoints, "DataSet 1");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setValueTextColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setDrawCircleHole(false);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        LineData lineData = new LineData(dataSet);
        mGraphView.setData(lineData);

        mGraphView.invalidate();
        // get the legend (only possible after setting data)
        Legend l = mGraphView.getLegend();
        l.setEnabled(false);

        XAxis xAxis = mGraphView.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setCenterAxisLabels(true);
        xAxis.setLabelCount(3);
        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM yy HH:mm", Locale.US);

            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                //long millis = TimeUnit.HOURS.toMillis((long) value);
                return mFormat.format(new Date((long) value));
            }
        });

        YAxis leftAxis = mGraphView.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(170f);
        leftAxis.setYOffset(-9f);

        YAxis rightAxis = mGraphView.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void initGraph() {
        // no description text
        mGraphView.getDescription().setEnabled(false);
        mGraphView.setContentDescription(getString(R.string.content_description_stock_graph));

        // enable touch gestures
        mGraphView.setTouchEnabled(true);

        mGraphView.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        mGraphView.setDragEnabled(true);
        mGraphView.setScaleEnabled(true);
        mGraphView.setDrawGridBackground(false);
        mGraphView.setHighlightPerDragEnabled(true);

        // set an alternative background color
        mGraphView.setBackgroundColor(Color.WHITE);
        mGraphView.setViewPortOffsets(0f, 0f, 0f, 0f);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
