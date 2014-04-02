package io.kickflip.sample.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.List;

import io.kickflip.sample.R;
import io.kickflip.sample.SECRETS;
import io.kickflip.sample.adapter.StreamAdapter;
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
public class StreamListFragment extends Fragment implements AbsListView.OnItemClickListener {

    private StreamListFragmentInteractionListener mListener;
    private KickflipApiClient mKickflip;
    private List<Stream> mStreams;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private StreamAdapter mAdapter;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StreamListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKickflip = new KickflipApiClient(getActivity(), SECRETS.CLIENT_KEY, SECRETS.CLIENT_SECRET);
        getStreams();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stream, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (StreamListFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement StreamListFragmentInteractionListener");
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

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse(stream.getStreamUrl()), "application/vnd.apple.mpegurl");
        startActivity(i);
    }

    private void getStreams() {
        mKickflip.getBroadcastsByKeyword(mKickflip.getCachedUser(), "test", new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                Log.i("API", "request succeeded " + response);
                if (getActivity() != null) {
                    mStreams = ((StreamList) response).getStreams();
                    if (mStreams.size() == 0) {
                        showNoBroadcasts();
                    } else {
                        mAdapter = new StreamAdapter(getActivity(), mStreams);
                        mListView.setAdapter(mAdapter);
                    }
                }

            }

            @Override
            public void onError(Object response) {
                Log.i("API", "request failed " + response);
                if (getActivity() != null) {
                    showNetworkError();
                }
            }
        });
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface StreamListFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

}
