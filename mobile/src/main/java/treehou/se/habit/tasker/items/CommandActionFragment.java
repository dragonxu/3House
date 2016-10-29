package treehou.se.habit.tasker.items;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.trello.rxlifecycle.components.support.RxFragment;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import se.treehou.ng.ohcommunicator.connector.models.OHItem;
import se.treehou.ng.ohcommunicator.services.Connector;
import se.treehou.ng.ohcommunicator.services.IServerHandler;
import se.treehou.ng.ohcommunicator.services.callbacks.OHCallback;
import se.treehou.ng.ohcommunicator.services.callbacks.OHResponse;
import treehou.se.habit.R;
import treehou.se.habit.core.db.model.ItemDB;
import treehou.se.habit.core.db.model.ServerDB;
import treehou.se.habit.tasker.boundle.CommandBoundleManager;

public class CommandActionFragment extends RxFragment {

    private Spinner sprItems;
    private TextView txtCommand;

    private ArrayAdapter<OHItem> itemAdapter;
    private List<OHItem> filteredItems = new ArrayList<>();

    private Realm realm;

    public static CommandActionFragment newInstance() {
        CommandActionFragment fragment = new CommandActionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public CommandActionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_tasker_command_action, container, false);

        sprItems = (Spinner) rootView.findViewById(R.id.spr_items);

        itemAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, filteredItems);
        sprItems.post(() -> sprItems.setAdapter(itemAdapter));
        RealmResults<ServerDB> servers = realm.where(ServerDB.class).findAll();
        filteredItems.clear();

        for(final ServerDB server : servers) {
            IServerHandler serverHandler = new Connector.ServerHandler(server.toGeneric(), getActivity());
            serverHandler.requestItemsRx()
                    .map(this::filterItems)
                    .compose(bindToLifecycle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(items -> {
                        filteredItems.addAll(items);
                        itemAdapter.notifyDataSetChanged();
                    });
        }

        txtCommand = (TextView) rootView.findViewById(R.id.txt_command);

        Button btnSave = (Button) rootView.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            save();
            getActivity().finish();
        });

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private List<OHItem> filterItems(List<OHItem> items){

        List<OHItem> tempItems = new ArrayList<>();
        tempItems.addAll(items);
        items.clear();
        items.addAll(tempItems);

        return items;
    }

    public void save() {

        final Intent resultIntent = new Intent();

        OHItem item = (OHItem) sprItems.getSelectedItem();
        realm.beginTransaction();
        ItemDB itemDb = ItemDB.createOrLoadFromGeneric(realm, item);
        realm.commitTransaction();

        String command = txtCommand.getText().toString();
        final Bundle resultBundle = CommandBoundleManager.generateCommandBundle(itemDb.getId(), command);

        resultIntent.putExtra(treehou.se.habit.tasker.locale.Intent.EXTRA_STRING_BLURB, item.getName() + " - " + command);
        resultIntent.putExtra(treehou.se.habit.tasker.locale.Intent.EXTRA_BUNDLE, resultBundle);

        getActivity().setResult(Activity.RESULT_OK, resultIntent);
    }
}
