package treehou.se.habit.ui.sitemaps;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import se.treehou.ng.ohcommunicator.connector.models.OHServer;
import se.treehou.ng.ohcommunicator.connector.models.OHSitemap;
import se.treehou.ng.ohcommunicator.services.Connector;
import se.treehou.ng.ohcommunicator.services.IServerHandler;
import se.treehou.ng.ohcommunicator.services.callbacks.OHCallback;
import se.treehou.ng.ohcommunicator.services.callbacks.OHResponse;
import treehou.se.habit.R;
import treehou.se.habit.ui.BaseFragment;
import treehou.se.habit.ui.adapter.SitemapAdapter;
import treehou.se.habit.util.ConnectionFactory;
import treehou.se.habit.util.Util;

public class SitemapSelectorFragment extends BaseFragment {

    private static final String TAG = "SitemapSelectorFragment";

    @BindView(R.id.list) RecyclerView mListView;

    @Inject ConnectionFactory connectionFactory;

    private SitemapAdapter mSitemapAdapter;
    private Unbinder unbinder;

    public static SitemapSelectorFragment newInstance() {
        SitemapSelectorFragment fragment = new SitemapSelectorFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SitemapSelectorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Util.getApplicationComponent(this).inject(this);

        mSitemapAdapter = new SitemapAdapter();
        mSitemapAdapter.setSelectorListener(sitemapSelectListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_sitemap_selector, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 1);

        mListView.setLayoutManager(gridLayoutManager);
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mSitemapAdapter);

        return rootView;
    }

    private SitemapAdapter.OnSitemapSelectListener sitemapSelectListener = new SitemapAdapter.OnSitemapSelectListener() {
        @Override
        public void onSitemapSelect(OHSitemap sitemap) {
            if(getTargetFragment() != null){
                ((SitemapAdapter.OnSitemapSelectListener) getTargetFragment()).onSitemapSelect(sitemap);
            }else {
                ((SitemapAdapter.OnSitemapSelectListener) getActivity()).onSitemapSelect(sitemap);
            }
        }

        @Override
        public void onErrorClicked(OHServer server) {
            requestSitemap(server);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        mSitemapAdapter.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    /**
     * Request sitemaps for server.
     * @param server the server to request sitemap for.
     */
    private void requestSitemap(final OHServer server){
        mSitemapAdapter.setServerState(server, SitemapAdapter.SitemapItem.STATE_LOADING);
        IServerHandler serverHandler = connectionFactory.createServerHandler(server, getContext());

        serverHandler.requestSitemapRx()
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sitemaps -> {
                    for (OHSitemap sitemap : sitemaps) {
                        sitemap.setServer(server);
                        if (!mSitemapAdapter.contains(sitemap)) {
                            mSitemapAdapter.add(sitemap);
                        } else if (OHSitemap.isLocal(sitemap)) {
                            mSitemapAdapter.remove(sitemap);
                            mSitemapAdapter.add(sitemap);
                        }
                    }
                    mSitemapAdapter.notifyDataSetChanged();
                }, throwable -> {
                    mSitemapAdapter.setServerState(server, SitemapAdapter.SitemapItem.STATE_ERROR);
                });
    }
}
