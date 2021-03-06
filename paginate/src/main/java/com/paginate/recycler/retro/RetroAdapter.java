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
	private int internalStartLoadingRow = -1;
	private int internalEndLoadingRow = -1;
	
	public RetroAdapter(RecyclerView.Adapter adapter, LoadingListItemCreator creator) {
		this.wrappedAdapter = adapter;
		this.loadingListItemCreator = creator;
		setHasStableIds(wrappedAdapter.hasStableIds());
		wrappedAdapter.registerAdapterDataObserver(dataObserver);
	}
	
	private final RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
		@Override
		public void onChanged() {
			notifyDataSetChanged();
		}
		
		@Override
		public void onItemRangeInserted(int positionStart, int itemCount) {
			notifyItemRangeInserted(getItemPositionFromAdapter(positionStart), itemCount);
		}
		
		@Override
		public void onItemRangeChanged(int positionStart, int itemCount) {
			notifyItemRangeChanged(getItemPositionFromAdapter(positionStart), itemCount);
		}
		
		@Override
		public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
			notifyItemRangeChanged(getItemPositionFromAdapter(positionStart), itemCount, payload);
		}
		
		@Override
		public void onItemRangeRemoved(int positionStart, int itemCount) {
			notifyItemRangeRemoved(getItemPositionFromAdapter(positionStart), itemCount);
		}
		
		@Override
		public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
			notifyItemMoved(getItemPositionFromAdapter(fromPosition), getItemPositionFromAdapter(toPosition));
		}
	};
	
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
		additional += hasInternalStartLoadingRow() ? 1 : 0;
		additional += hasInternalEndLoadingRow() ? 1 : 0;
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
	
	
	public RecyclerView.Adapter getWrappedAdapter() {
		return wrappedAdapter;
	}
	
	public int getItemPositionInAdapter(int index) {
		int decrease = 0;
		decrease += startLoadingRow ? 1 : 0;
		if (hasInternalStartLoadingRow()) {
			decrease += index > internalStartLoadingRow ? 1 : 0;
		}
		if (hasInternalEndLoadingRow()) {
			decrease += index > internalEndLoadingRow ? 1 : 0;
		}
		return index - decrease;
	}
	
	public int getItemPositionFromAdapter(int index) {
		int increase = 0;
		increase += startLoadingRow ? 1 : 0;
		if (hasInternalStartLoadingRow()) {
			increase += index > internalStartLoadingRow ? 1 : 0;
		}
		if (hasInternalEndLoadingRow()) {
			increase += index > internalEndLoadingRow ? 1 : 0;
		}
		return index + increase;
	}
	
	private boolean isEndLoadingRow(final int index) {
		return getEndLoadingRowPosition() == index || getEndInternalLoadingRowPosition() == index;
	}
	
	private boolean isStartLoadingRow(final int index) {
		return getStartLoadingRowPosition() == index || getStartInternalLoadingRowPosition() == index;
	}
	
	public int getEndLoadingRowPosition() {
		if (!endLoadingRow) {
			return -1;
		}
		int position = wrappedAdapter.getItemCount();
		position += startLoadingRow ? 1 : 0;
		position += hasInternalStartLoadingRow() ? 1 : 0;
		position += hasInternalEndLoadingRow() ? 1 : 0;
		return position;
	}
	
	public int getEndInternalLoadingRowPosition() {
		if (!hasInternalEndLoadingRow()) {
			return -1;
		}
		int position = internalEndLoadingRow;
		position += startLoadingRow ? 1 : 0;
		position += hasInternalStartLoadingRow() ? 1 : 0;
		return position;
	}
	
	public int getStartLoadingRowPosition() {
		if (!startLoadingRow) {
			return -1;
		}
		return 0;
	}
	
	public int getStartInternalLoadingRowPosition() {
		if (!hasInternalStartLoadingRow()) {
			return -1;
		}
		int position = internalStartLoadingRow;
		position += startLoadingRow ? 1 : 0;
		return position;
	}
	
	private boolean hasInternalStartLoadingRow() {
		return internalStartLoadingRow >= 0;
	}
	
	private boolean hasInternalEndLoadingRow() {
		return internalEndLoadingRow >= 0;
	}
	
	public boolean isLoadingRow(int position) {
		return isStartLoadingRow(position) || isEndLoadingRow(position);
	}
	
	public int getInternalStartLoadingRow() {
		return internalStartLoadingRow;
	}
	
	public int getInternalEndLoadingRow() {
		return internalEndLoadingRow;
	}
	
	public void displayStartLoadingRow(boolean startLoadingRow) {
		if (this.startLoadingRow != startLoadingRow) {
			this.startLoadingRow = startLoadingRow;
			if (startLoadingRow) {
				notifyItemInserted(0);
			} else {
				notifyItemRemoved(0);
			}
		}
	}
	
	public void displayEndLoadingRow(boolean endLoadingRow) {
		if (this.endLoadingRow != endLoadingRow) {
			this.endLoadingRow = endLoadingRow;
			if (endLoadingRow) {
				notifyItemInserted(getItemCount() - 1);
			} else {
				notifyItemRemoved(getItemCount() - 1);
			}
		}
	}
	
	public void displayInternalStartLoadingRow(int internalStartLoadingRow) {
		if (this.internalStartLoadingRow != internalStartLoadingRow) {
			int oldRow = this.internalStartLoadingRow;
			this.internalStartLoadingRow = internalStartLoadingRow;
			if (internalStartLoadingRow >= 0) {
				notifyItemInserted(getItemPositionFromAdapter(internalStartLoadingRow));
			}
			if (oldRow >= 0) {
				notifyItemRemoved(getItemPositionFromAdapter((oldRow)));
			}
			
		}
		
	}
	
	public void displayInternalEndLoadingRow(int internalEndLoadingRow) {
		if (this.internalEndLoadingRow != internalEndLoadingRow) {
			int oldRow = this.internalEndLoadingRow;
			this.internalEndLoadingRow = internalEndLoadingRow;
			if (internalEndLoadingRow >= 0) {
				notifyItemInserted(getItemPositionFromAdapter(internalEndLoadingRow));
			}
			if (oldRow >= 0) {
				notifyItemRemoved(getItemPositionFromAdapter(oldRow));
			}
		}
		
	}
}