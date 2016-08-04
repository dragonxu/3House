package treehou.se.habit.ui.servers.sitemaps;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.support.RxFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;
import se.treehou.ng.ohcommunicator.connector.models.OHServer;
import se.treehou.ng.ohcommunicator.connector.models.OHSitemap;
import treehou.se.habit.R;
import treehou.se.habit.core.db.model.ServerDB;
import treehou.se.habit.core.db.model.SitemapDB;
import treehou.se.habit.ui.adapter.SitemapListAdapter;
import treehou.se.habit.util.RxConnectorUtil;
import treehou.se.habit.util.RxUtil;
import treehou.se.habit.util.Settings;

public class SitemapSelectFragment extends RxFragment {

    private static final String TAG = "SitemapSelectFragment";

    private static final String ARG_SHOW_SERVER = "ARG_SHOW_SERVER";

    @Inject Settings settings;
    @Inject RxConnectorUtil rxConnectorUtil;

    @BindView(R.id.list) RecyclerView listView;
    @BindView(R.id.empty) TextView emptyView;
    private SitemapListAdapter sitemapAdapter;
    private Realm realm;
    private Unbinder unbinder;
    private BehaviorSubject<OHServer> serverBehaviorSubject = BehaviorSubject.create();
    private long serverId = -1;

    /**
     * Load sitemaps for servers.
     * Open provided sitemap if loaded.
     *
     * @param serverId the server to load
     * @return Fragment
     */
    public static SitemapSelectFragment newInstance(long serverId) {
        SitemapSelectFragment fragment = new SitemapSelectFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SHOW_SERVER, serverId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SitemapSelectFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getDefaultInstance();
        serverId = getArguments().getLong(ARG_SHOW_SERVER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sitemaplist, container, false);
        unbinder = ButterKnife.bind(this, view);
        setupActionBar();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 1);
        listView.setLayoutManager(gridLayoutManager);
        listView.setItemAnimator(new DefaultItemAnimator());

        sitemapAdapter = new SitemapListAdapter(getContext());
        sitemapAdapter.setSitemapSelectedListener(new SitemapListAdapter.SitemapSelectedListener() {

            @Override
            public void onSelected(OHServer server, OHSitemap sitemap) {
                SitemapDB sitemapDB = realm.where(SitemapDB.class)
                        .equalTo("server.id", serverId)
                        .equalTo("name", sitemap.getName())
                        .findFirst();

                if(sitemapDB != null){
                    openSitemap(sitemapDB);
                }
            }

            @Override
            public void onErrorSelected(OHServer server) {
                serverBehaviorSubject.onNext(server);
            }
        });
        listView.setAdapter(sitemapAdapter);

        return view;
    }

    /**
     * Open fragment showing sitemap.
     *
     * @param sitemap the name of sitemap to show.
     */
    private void openSitemap(SitemapDB sitemap){
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.page_container, SitemapSettingsFragment.newInstance(sitemap.getId()))
                .addToBackStack(null)
                .commit();
    }

    /**
     * Setup actionbar.
     */
    private void setupActionBar(){
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) actionBar.setTitle(R.string.sitemaps);
    }

    /**
     * Clears list of sitemaps.
     */
    private void clearList() {
        emptyView.setVisibility(View.VISIBLE);
        sitemapAdapter.clear();

    }

    @Override
    public void onResume() {
        super.onResume();

        clearList();
        loadSitemapsFromServers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    /**
     * Load servers from database and request their sitemaps.
     */
    private void loadSitemapsFromServers(){
        realm.where(ServerDB.class).equalTo("id", serverId).findAll().asObservable()
                .flatMap(Observable::from)
                .map(ServerDB::toGeneric)
                .distinct()
                .compose(rxConnectorUtil.serverToSitemap(getActivity()))
                .observeOn(AndroidSchedulers.mainThread())
                .compose(RxLifecycle.bindFragment(this.lifecycle()))
                .subscribe(serverSitemaps -> {
                    emptyView.setVisibility(View.GONE);

                    OHServer server = serverSitemaps.first;
                    List<OHSitemap> sitemaps = serverSitemaps.second;

                    for (OHSitemap sitemap : sitemaps) {
                        sitemapAdapter.add(server, sitemap);
                    }
                }, throwable -> {
                    Log.e(TAG, "Request sitemap failed", throwable);
                });
    }
}
