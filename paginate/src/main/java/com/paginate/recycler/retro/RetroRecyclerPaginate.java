package com.paginate.recycler.retro;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.paginate.Paginate;
import com.paginate.recycler.DefaultLoadingListItemSpanLookup;
import com.paginate.recycler.LoadingListItemCreator;
import com.paginate.recycler.LoadingListItemSpanLookup;

public final class RetroRecyclerPaginate extends Paginate {

	private final RecyclerView recyclerView;
	private final RetroCallbacks callbacks;
	private final RetroCallbacks internalCallbacks;
	private final int loadingTriggerThreshold;
	private RetroAdapter wrapperAdapter;
	private RetroWrapperSpanSizeLookup wrapperSpanSizeLookup;

	RetroRecyclerPaginate(RecyclerView recyclerView,
	                      RetroCallbacks callbacks,
	                      RetroCallbacks internalCallbacks,
	                      int loadingTriggerThreshold,
	                      boolean addLoadingListItem,
	                      LoadingListItemCreator loadingListItemCreator,
	                      LoadingListItemSpanLookup loadingListItemSpanLookup) {
		this.recyclerView = recyclerView;
		this.callbacks = callbacks;
		this.internalCallbacks = internalCallbacks;
		this.loadingTriggerThreshold = loadingTriggerThreshold;

		// Attach scrolling listener in order to perform end offset check on each scroll event
		recyclerView.addOnScrollListener(mOnScrollListener);

		if (addLoadingListItem) {
			// Wrap existing adapter with new adapter that will add loading row
			RecyclerView.Adapter adapter = recyclerView.getAdapter();
			wrapperAdapter = new RetroAdapter(adapter, loadingListItemCreator);
			recyclerView.setAdapter(wrapperAdapter);

			// For GridLayoutManager use separate/customisable span lookup for loading row
			if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
				wrapperSpanSizeLookup = new RetroWrapperSpanSizeLookup(
						((GridLayoutManager) recyclerView.getLayoutManager()).getSpanSizeLookup(),
						loadingListItemSpanLookup,
						wrapperAdapter);
				((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(wrapperSpanSizeLookup);
			}
		}

	}


	@Override
	public void setHasMoreDataToLoad(boolean hasMoreDataToLoad) {
		if (wrapperAdapter != null) {
			wrapperAdapter.displayEndLoadingRow(hasMoreDataToLoad);
		}
	}

	public void setHasMoreDataToLoadOnStart(boolean hasMoreDataToLoad) {
		if (wrapperAdapter != null) {
			wrapperAdapter.displayStartLoadingRow(hasMoreDataToLoad);
		}
	}

	public void setHasMoreInternalDataToLoad(int internalLoadPosition) {
		if (wrapperAdapter != null) {
			wrapperAdapter.displayInternalEndLoadingRow(internalLoadPosition);
		}
	}

	public void setHasMoreInternalDataToLoadOnStart(int internalLoadPosition) {
		if (wrapperAdapter != null) {
			wrapperAdapter.displayInternalStartLoadingRow(internalLoadPosition);
		}
	}

	public int getInternalLoadPosition() {
		return wrapperAdapter.getInternalEndLoadingRow();
	}

	public int getInternalStartLoadPosition() {
		return wrapperAdapter.getInternalStartLoadingRow();
	}

	public int mapPositions(int position) {
		return wrapperAdapter.getItemPositionInAdapter(position);
	}

	public int mapPositionsReverse(int position) {
		return wrapperAdapter.getItemPositionFromAdapter(position);
	}

	public void registerObserver(RecyclerView.AdapterDataObserver observer) {
		wrapperAdapter.registerAdapterDataObserver(observer);
	}

	public void unregisterObserver(RecyclerView.AdapterDataObserver observer) {
		wrapperAdapter.unregisterAdapterDataObserver(observer);
	}

	@Override
	public void unbind() {
		recyclerView.removeOnScrollListener(mOnScrollListener);   // Remove scroll listener
		if (recyclerView.getAdapter() instanceof RetroAdapter) {
			RetroAdapter wrapperAdapter = (RetroAdapter) recyclerView.getAdapter();
			RecyclerView.Adapter adapter = wrapperAdapter.getWrappedAdapter();
			recyclerView.setAdapter(adapter);                     // Swap back original adapter
		}
		if (recyclerView.getLayoutManager() instanceof GridLayoutManager && wrapperSpanSizeLookup != null) {
			// Swap back original SpanSizeLookup
			GridLayoutManager.SpanSizeLookup spanSizeLookup = wrapperSpanSizeLookup.getWrappedSpanSizeLookup();
			((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(spanSizeLookup);
		}
	}

	void checkOffset() {
		int totalItemCount = recyclerView.getLayoutManager().getItemCount();

		if (totalItemCount == 0) {
			return;
		}

		int visibleItemEndPosition = 0;
		int visibleItemStartPosition = 0;

		if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
			LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			visibleItemEndPosition = linearLayoutManager.findLastVisibleItemPosition();
			visibleItemStartPosition = linearLayoutManager.findFirstVisibleItemPosition();

		} else if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
			// https://code.google.com/p/android/issues/detail?id=181461
			if (recyclerView.getLayoutManager().getChildCount() > 0) {
				StaggeredGridLayoutManager gridLayoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
				visibleItemEndPosition = gridLayoutManager.findLastVisibleItemPositions(null)
						[0];
				visibleItemStartPosition = gridLayoutManager.findFirstVisibleItemPositions(null)
						[0];
			}
		} else {
			throw new IllegalStateException("LayoutManager needs to subclass LinearLayoutManager or StaggeredGridLayoutManager");
		}

		// Check if end of the list is reached (counting threshold) or if there is no items at all
		
		int loadingRowPosition = wrapperAdapter.getEndLoadingRowPosition();
		if (loadingRowPosition != -1 &&
				(loadingRowPosition <= (visibleItemEndPosition + loadingTriggerThreshold))) {
			// Call load more only if loading is not currently in progress and if there is more items to load
			if (!callbacks.isLoading() && !callbacks.hasLoadedAllItems()) {
				callbacks.onLoadMore();
			}
		}
		
		loadingRowPosition = wrapperAdapter.getStartLoadingRowPosition();
		if (loadingRowPosition != -1 &&
				(loadingRowPosition >= (visibleItemStartPosition - loadingTriggerThreshold))) {
			// Call load more only if loading is not currently in progress and if there is more items to load
			if (!callbacks.isLoadingFromStart() && !callbacks.hasLoadedAllItemsFromStart()) {
				callbacks.onLoadMoreFromStart();
			}
		}

		if (internalCallbacks != null) {
			// Check if end of the list is reached (counting threshold) or if there is no items at all
			if (wrapperAdapter.getEndInternalLoadingRowPosition() <= (visibleItemEndPosition + loadingTriggerThreshold)) {
				// Call load more only if loading is not currently in progress and if there is more items to load
				if (!internalCallbacks.isLoading() && !internalCallbacks.hasLoadedAllItems()) {
					internalCallbacks.onLoadMore();
				}
			} else if (wrapperAdapter.getStartInternalLoadingRowPosition() >= (visibleItemStartPosition - loadingTriggerThreshold)) {
				// Call load more only if loading is not currently in progress and if there is more items to load
				if (!internalCallbacks.isLoadingFromStart() && !internalCallbacks.hasLoadedAllItemsFromStart()) {
					internalCallbacks.onLoadMoreFromStart();
				}
			}
		}
	}

	private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			checkOffset(); // Each time when list is scrolled check if end of the list is reached
		}
	};

	public static class Builder {

		private final RecyclerView recyclerView;
		private final RetroCallbacks callbacks;
		private RetroCallbacks internalCallbacks;

		private int loadingTriggerThreshold = 5;
		private boolean addLoadingListItem = true;
		private LoadingListItemCreator loadingListItemCreator;
		private LoadingListItemSpanLookup loadingListItemSpanLookup;

		public Builder(RecyclerView recyclerView, RetroCallbacks callbacks) {
			this.recyclerView = recyclerView;
			this.callbacks = callbacks;
		}

		public Builder setInternalCallbacks(RetroCallbacks internalCallbacks) {
			this.internalCallbacks = internalCallbacks;
			return this;
		}

		/**
		 * Set the offset from the end of the list at which the load more event needs to be triggered. Default offset
		 * if 5.
		 *
		 * @param threshold number of items from the end of the list.
		 * @return {@link RetroRecyclerPaginate.Builder}
		 */
		public Builder setLoadingTriggerThreshold(int threshold) {
			this.loadingTriggerThreshold = threshold;
			return this;
		}

		/**
		 * Setup loading row. If loading row is used original adapter set on RecyclerView will be wrapped with
		 * internal adapter that will add loading row as the last item in the list. Paginate will observer the
		 * changes upon original adapter and remove loading row if there is no more data to load. By default loading
		 * row will be added.
		 *
		 * @param addLoadingListItem true if loading row needs to be added, false otherwise.
		 * @return {@link RetroRecyclerPaginate.Builder}
		 * @see {@link Callbacks#hasLoadedAllItems()}
		 * @see {@link RetroRecyclerPaginate.Builder#setLoadingListItemCreator(LoadingListItemCreator)}
		 */
		public Builder addLoadingListItem(boolean addLoadingListItem) {
			this.addLoadingListItem = addLoadingListItem;
			return this;
		}

		/**
		 * Set custom loading list item creator. If no creator is set default one will be used.
		 *
		 * @param creator Creator that will ne called for inflating and binding loading list item.
		 * @return {@link RetroRecyclerPaginate.Builder}
		 */
		public Builder setLoadingListItemCreator(LoadingListItemCreator creator) {
			this.loadingListItemCreator = creator;
			return this;
		}

		/**
		 * Set custom SpanSizeLookup for loading list item. Use this when {@link GridLayoutManager} is used and
		 * loading list item needs to have custom span. Full span of {@link GridLayoutManager} is used by default.
		 *
		 * @param loadingListItemSpanLookup LoadingListItemSpanLookup that will be called for loading list item span.
		 * @return {@link RetroRecyclerPaginate.Builder}
		 */
		public Builder setLoadingListItemSpanSizeLookup(LoadingListItemSpanLookup loadingListItemSpanLookup) {
			this.loadingListItemSpanLookup = loadingListItemSpanLookup;
			return this;
		}

		/**
		 * Create pagination functionality upon RecyclerView.
		 *
		 * @return {@link Paginate} instance.
		 */
		public RetroRecyclerPaginate build() {
			if (recyclerView.getAdapter() == null) {
				throw new IllegalStateException("Adapter needs to be set!");
			}
			if (recyclerView.getLayoutManager() == null) {
				throw new IllegalStateException("LayoutManager needs to be set on the RecyclerView");
			}

			if (loadingListItemCreator == null) {
				loadingListItemCreator = LoadingListItemCreator.DEFAULT;
			}

			if (loadingListItemSpanLookup == null) {
				loadingListItemSpanLookup = new DefaultLoadingListItemSpanLookup(recyclerView.getLayoutManager());
			}

			return new RetroRecyclerPaginate(recyclerView, callbacks, internalCallbacks, loadingTriggerThreshold, addLoadingListItem,
					loadingListItemCreator, loadingListItemSpanLookup);
		}
	}

	public interface RetroCallbacks extends Callbacks {

		void onLoadMoreFromStart();

		boolean isLoadingFromStart();

		boolean hasLoadedAllItemsFromStart();
	}

}