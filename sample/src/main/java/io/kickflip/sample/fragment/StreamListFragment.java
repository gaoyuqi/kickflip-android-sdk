package io.kickflip.sample.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.kickflip.sample.R;
import io.kickflip.sample.SECRETS;
import io.kickflip.sample.adapter.StreamAdapter;
import io.kickflip.sdk.Share;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.StreamList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class StreamListFragment extends Fragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = "StreamListFragment";
    private static final boolean VERBOSE = true;

    private StreamListFragmenListener mListener;
    private SwipeRefreshLayout mSwipeLayout;
    private KickflipApiClient mKickflip;
    private List<Stream> mStreams;
    private boolean mRefreshing;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private StreamAdapter mAdapter;

    private StreamAdapter.StreamAdapterActionListener mStreamActionListener = new StreamAdapter.StreamAdapterActionListener() {
        @Override
        public void onFlagButtonClick(final Stream stream) {
            // Flag recording
            mKickflip.flagStream(stream, new KickflipCallback() {
                @Override
                public void onSuccess(Response response) {
                    if (getActivity() != null) {
                        if (mKickflip.getCachedUser().getName().compareTo(stream.getOwnerName()) == 0) {
                            mAdapter.remove(stream);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.stream_flagged), Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onError(Object response) {}
            });
        }

        @Override
        public void onShareButtonClick(Stream stream) {
            Intent shareIntent = Share.createShareChooserIntentWithTitleAndUrl(getActivity(), getString(io.kickflip.sdk.R.string.share_broadcast), stream.getKickflipUrl());
            startActivity(shareIntent);
        }
    };


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StreamListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKickflip = new KickflipApiClient(getActivity(), SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (mAdapter != null) {
                    mAdapter.setUserName(mKickflip.getCachedUser().getName());
                }
                // Update profile display when we add that
            }

            @Override
            public void onError(Object response) {
                showNetworkError();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getStreams();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stream, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        // Why does this selection remain if I long press, release
        // without activating onListItemClick?
        //mListView.setSelector(R.drawable.stream_list_selector_overlay);
        //mListView.setDrawSelectorOnTop(true);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorScheme(R.color.kickflip_green,
                R.color.kickflip_green_shade_2,
                R.color.kickflip_green_shade_3,
                R.color.kickflip_green_shade_4);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        setupListViewAdapter();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (StreamListFragmenListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement StreamListFragmenListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Stream stream = mAdapter.getItem(position);
        mListener.onStreamPlaybackRequested(stream.getStreamUrl());
    }

    private void getStreams() {
        mRefreshing = true;
        mKickflip.getBroadcastsByKeyword(mKickflip.getCachedUser(), null, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (VERBOSE) Log.i("API", "request succeeded " + response);
                if (getActivity() != null) {
                    mStreams = ((StreamList) response).getStreams();
                    Collections.sort(mStreams);
                    mAdapter.clear();
                    mAdapter.addAll(mStreams);
                    mAdapter.notifyDataSetChanged();
                    if (mStreams.size() == 0) {
                        showNoBroadcasts();
                    }
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
            }

            @Override
            public void onError(Object response) {
                if (VERBOSE) Log.i("API", "request failed " + response);
                if (getActivity() != null) {
                    showNetworkError();
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
            }
        });
    }

    private void setupListViewAdapter() {
        mStreams = new ArrayList<>(0);
        mAdapter = new StreamAdapter(getActivity(), mStreams, mStreamActionListener);
        mListView.setAdapter(mAdapter);
        if (mKickflip.credentialsAcquired()) {
            mAdapter.setUserName(mKickflip.getCachedUser().getName());
        }
    }

    /**
     * Inform the user that a network error has occured
     */
    public void showNetworkError() {
        setEmptyListViewText(getString(R.string.no_network));
    }

    /**
     * Inform the user that no broadcasts were found
     */
    public void showNoBroadcasts() {
        setEmptyListViewText(getString(R.string.no_broadcasts));
    }

    /**
     * If the ListView is hidden, show the
     *
     * @param text
     */
    private void setEmptyListViewText(String text) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(text);
        }
    }

    @Override
    public void onRefresh() {
        if (!mRefreshing) {
            getStreams();
        }

    }

    public interface StreamListFragmenListener {
        public void onStreamPlaybackRequested(String url);
    }

}
