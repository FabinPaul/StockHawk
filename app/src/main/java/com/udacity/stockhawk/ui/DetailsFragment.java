package com.udacity.stockhawk.ui;

import android.database.Cursor;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String BUNDLE_EXTRA_SYMBOL = "com.udacity.stockhawk.BUNDLE_EXTRA_SYMBOL";
    private static final int STOCK_LOADER = 1;
    private static final String TAG = DetailsFragment.class.getSimpleName();
    private Unbinder mUnbinder;

    @BindView(R.id.stock_graph)
    GraphView mGraphView;

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
            String[] listOfHistoryItems = historyItems.split("\\n");
            DataPoint[] dataPoints = new DataPoint[listOfHistoryItems.length];
            Date minDate = null, maxDate = null;
            float minValue = Float.MAX_VALUE, maxValue = Float.MIN_VALUE;
            for (int i = 0; i < listOfHistoryItems.length; i++) {
                String partsOfItem[] = listOfHistoryItems[i].split(", ");
                long timeStamp = Long.parseLong(partsOfItem[0]);
                Date date = new Date(timeStamp);
                if (i == 0)
                    maxDate = date;
                if (i == listOfHistoryItems.length - 1)
                    minDate = date;
                float value = Float.parseFloat(partsOfItem[1]);
//                dataPoints[i] = new DataPoint(date, value);
                if (value < minValue)
                    minValue = value;
                if (value > maxValue)
                    maxValue = value;
                dataPoints[i] = new DataPoint(date, value);
            }
            PointsGraphSeries<DataPoint> series = new PointsGraphSeries<DataPoint>(dataPoints);
            mGraphView.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity()));
            mGraphView.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space

            // set manual x bounds to have nice steps
            if (null != minDate && null != maxDate) {
                mGraphView.getViewport().setMinX(minDate.getTime());
                mGraphView.getViewport().setMaxX(maxDate.getTime());
            }

            mGraphView.getViewport().setMinY(minValue);
            mGraphView.getViewport().setMaxY(maxValue);

            mGraphView.getViewport().setXAxisBoundsManual(true);

            // as we use dates as labels, the human rounding to nice readable numbers
            // is not necessary
            mGraphView.getGridLabelRenderer().setHumanRounding(false);

            mGraphView.getViewport().setScrollable(true); // enables horizontal scrolling
            mGraphView.addSeries(series);
            Log.d(TAG, historyItems);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
