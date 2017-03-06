package com.paginate.recycler.retro;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.paginate.recycler.LoadingListItemCreator;

class RetroAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int ITEM_VIEW_TYPE_LOADING = Integer.MAX_VALUE - 50; // Magic

	private final RecyclerView.Adapter wrappedAdapter;
	private final LoadingListItemCreator loadingListItemCreator;
	private boolean startLoadingRow;
	private boolean endLoadingRow;

	public RetroAdapter(RecyclerView.Adapter adapter, LoadingListItemCreator creator) {
		this.wrappedAdapter = adapter;
		this.loadingListItemCreator = creator;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (viewType == ITEM_VIEW_TYPE_LOADING) {
			return loadingListItemCreator.onCreateViewHolder(parent, viewType);
		} else {
			return wrappedAdapter.onCreateViewHolder(parent, viewType);
		}
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (isLoadingRow(position)) {
			loadingListItemCreator.onBindViewHolder(holder, position);
		} else {
			wrappedAdapter.onBindViewHolder(holder, getItemPositionInAdapter(position));
		}
	}


	@Override
	public int getItemCount() {
		int innerCount = wrappedAdapter.getItemCount();
		if (innerCount == 0) {
			return 0;
		}
		int additional = endLoadingRow ? 1 : 0;
		additional += startLoadingRow ? 1 : 0;
		return innerCount + additional;
	}

	@Override
	public int getItemViewType(int position) {
		return isLoadingRow(position)
				? ITEM_VIEW_TYPE_LOADING
				: wrappedAdapter.getItemViewType(getItemPositionInAdapter(position));
	}

	@Override
	public long getItemId(int position) {
		return isLoadingRow(position)
				? RecyclerView.NO_ID
				: wrappedAdapter.getItemId(getItemPositionInAdapter(position));
	}


	@Override
	public void setHasStableIds(boolean hasStableIds) {
		super.setHasStableIds(hasStableIds);
		wrappedAdapter.setHasStableIds(hasStableIds);
	}

	public RecyclerView.Adapter getWrappedAdapter() {
		return wrappedAdapter;
	}

	private int getItemPositionInAdapter(int index) {
		return startLoadingRow ? index - 1 : index;
	}

	private boolean isEndLoadingRow(final int index) {
		if (!endLoadingRow) {
			return false;
		}
		if (startLoadingRow) {
			return index >= wrappedAdapter.getItemCount() + 1;

		}
		return index >= wrappedAdapter.getItemCount();
	}

	private boolean isStartLoadingRow(final int index) {
		//noinspection SimplifiableIfStatement
		if (!startLoadingRow) {
			return false;
		}
		return index == 0;
	}

	public boolean isLoadingRow(int position) {
		return isStartLoadingRow(position) || isEndLoadingRow(position);
	}

	public boolean isStartLoadingRow() {
		return startLoadingRow;
	}

	public void displayStartLoadingRow(boolean startLoadingRow) {
		if (this.startLoadingRow != startLoadingRow) {
			this.startLoadingRow = startLoadingRow;
			notifyDataSetChanged();
		}
	}

	public boolean isEndLoadingRow() {
		return endLoadingRow;
	}

	public void displayEndLoadingRow(boolean endLoadingRow) {
		if (this.endLoadingRow != endLoadingRow) {
			this.endLoadingRow = endLoadingRow;
			notifyDataSetChanged();
		}
	}
}