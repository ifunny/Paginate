package com.paginate.recycler.retro;

import android.support.v7.widget.GridLayoutManager;

import com.paginate.recycler.LoadingListItemSpanLookup;

public class RetroWrapperSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

	private final GridLayoutManager.SpanSizeLookup wrappedSpanSizeLookup;
	private final LoadingListItemSpanLookup loadingListItemSpanLookup;
	private final RetroAdapter wrapperAdapter;

	public RetroWrapperSpanSizeLookup(GridLayoutManager.SpanSizeLookup gridSpanSizeLookup,
	                                  LoadingListItemSpanLookup loadingListItemSpanLookup,
	                                  RetroAdapter wrapperAdapter) {
		this.wrappedSpanSizeLookup = gridSpanSizeLookup;
		this.loadingListItemSpanLookup = loadingListItemSpanLookup;
		this.wrapperAdapter = wrapperAdapter;
	}

	@Override
	public int getSpanSize(int position) {
		if (wrapperAdapter.isLoadingRow(position)) {
			return loadingListItemSpanLookup.getSpanSize();
		} else {
			return wrappedSpanSizeLookup.getSpanSize(position);
		}
	}

	public GridLayoutManager.SpanSizeLookup getWrappedSpanSizeLookup() {
		return wrappedSpanSizeLookup;
	}
}