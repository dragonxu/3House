package treehou.se.habit.ui.control.config;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.realm.Realm;
import se.treehou.ng.ohcommunicator.connector.models.OHItem;
import se.treehou.ng.ohcommunicator.connector.models.OHServer;
import se.treehou.ng.ohcommunicator.services.Connector;
import se.treehou.ng.ohcommunicator.services.IServerHandler;
import se.treehou.ng.ohcommunicator.services.callbacks.OHCallback;
import se.treehou.ng.ohcommunicator.services.callbacks.OHResponse;
import treehou.se.habit.R;
import treehou.se.habit.core.controller.Cell;
import treehou.se.habit.core.db.model.ItemDB;
import treehou.se.habit.core.db.model.ServerDB;
import treehou.se.habit.core.db.model.controller.CellDB;
import treehou.se.habit.core.db.model.controller.VoiceCellDB;
import treehou.se.habit.util.Util;
import treehou.se.habit.ui.util.IconPickerActivity;

public class CellVoiceConfigFragment extends Fragment {

    private static final String TAG = "CellVoiceConfigFragment";

    private static String ARG_CELL_ID = "ARG_CELL_ID";
    private static int REQUEST_ICON = 183;

    @BindView(R.id.spr_items) Spinner sprItems;
    @BindView(R.id.btn_set_icon) ImageButton btnSetIcon;

    private VoiceCellDB voiceCell;
    private Cell cell;

    private OHItem item;

    private ArrayAdapter<OHItem> mItemAdapter;
    private ArrayList<OHItem> mItems = new ArrayList<>();

    private Realm realm;
    private Unbinder unbinder;

    public static CellVoiceConfigFragment newInstance(CellDB cell) {
        CellVoiceConfigFragment fragment = new CellVoiceConfigFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CELL_ID, cell.getId());
        fragment.setArguments(args);
        return fragment;
    }

    public CellVoiceConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getDefaultInstance();
        mItemAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, mItems);
        if (getArguments() != null) {
            long id = getArguments().getLong(ARG_CELL_ID);
            cell = new Cell(CellDB.load(realm, id));
            voiceCell = VoiceCellDB.getCell(realm, cell.getDB());
            if(voiceCell == null){
                realm.beginTransaction();
                voiceCell = new VoiceCellDB();
                voiceCell.setId(VoiceCellDB.getUniqueId(realm));
                voiceCell = realm.copyToRealm(voiceCell);
                voiceCell.setCell(cell.getDB());
                realm.commitTransaction();
            }

            ItemDB itemDB = voiceCell.getItem();
            if(itemDB != null){
                item = itemDB.toGeneric();
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_cell_voice_config, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        sprItems.setAdapter(mItemAdapter);

        List<ServerDB> servers = realm.where(ServerDB.class).findAll();
        mItems.clear();

        for(final ServerDB serverDB : servers) {
            final OHServer server = serverDB.toGeneric();
            OHCallback<List<OHItem>> callback = new OHCallback<List<OHItem>>() {

                @Override
                public void onUpdate(OHResponse<List<OHItem>> response) {
                    List<OHItem> items = filterItems(response.body());
                    mItems.addAll(items);
                    mItemAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError() {
                    Log.d("Get Items", "Failure");
                }
            };
            IServerHandler serverHandler = new Connector.ServerHandler(server, getContext());
            serverHandler.requestItem(callback);
        }

        sprItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                OHItem item = mItems.get(position);
                if(item != null) {
                    realm.beginTransaction();
                    ItemDB itemDB = ItemDB.createOrLoadFromGeneric(realm, item);
                    voiceCell.setItem(itemDB);
                    realm.commitTransaction();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if(item != null){
            mItems.add(item);
            mItemAdapter.add(item);
            mItemAdapter.notifyDataSetChanged();
        }

        updateIconImage();
        btnSetIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), IconPickerActivity.class);
            startActivityForResult(intent, REQUEST_ICON);
        });

        return rootView;
    }

    private List<OHItem> filterItems(List<OHItem> items){

        List<OHItem> tempItems = new ArrayList<>();
        for(OHItem item : items){
            if(item.getType().equals(OHItem.TYPE_STRING)){
                tempItems.add(item);
            }
        }
        items.clear();
        items.addAll(tempItems);

        return items;
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

    private void updateIconImage(){
        btnSetIcon.setImageDrawable(Util.getIconDrawable(getActivity(), voiceCell.getIcon()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ICON &&
                resultCode == Activity.RESULT_OK &&
                data.hasExtra(IconPickerActivity.RESULT_ICON)){

            realm.beginTransaction();
            String iconName = data.getStringExtra(IconPickerActivity.RESULT_ICON);
            voiceCell.setIcon(iconName.equals("") ? null : iconName);
            updateIconImage();
            realm.commitTransaction();
        }
    }
}
