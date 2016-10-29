package treehou.se.habit.ui.control.config;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
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
import treehou.se.habit.core.db.model.controller.SliderCellDB;
import treehou.se.habit.ui.BaseFragment;
import treehou.se.habit.util.Util;
import treehou.se.habit.ui.util.IconPickerActivity;

public class CellSliderConfigFragment extends BaseFragment {

    private static final String TAG = "CellSliderConfigFragment";
    
    private static String ARG_CELL_ID = "ARG_CELL_ID";
    private static int REQUEST_ICON = 183;

    @BindView(R.id.spr_items) Spinner sprItems;
    @BindView(R.id.txt_max) TextView txtMax;
    @BindView(R.id.btn_set_icon) ImageButton btnSetIcon;
    @BindView(R.id.lou_range) View louRange;

    private ArrayAdapter<OHItem> mItemAdapter ;
    private ArrayList<OHItem> items = new ArrayList<>();
    private SliderCellDB numberCell;
    private Cell cell;
    private OHItem item;
    private Unbinder unbinder;

    public static CellSliderConfigFragment newInstance(CellDB cell) {
        CellSliderConfigFragment fragment = new CellSliderConfigFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CELL_ID, cell.getId());
        fragment.setArguments(args);
        return fragment;
    }

    public CellSliderConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            long id = getArguments().getLong(ARG_CELL_ID);
            cell = new Cell(CellDB.load(realm, id));
            numberCell = SliderCellDB.getCell(realm, cell.getDB());

            if(numberCell == null){
                realm.beginTransaction();
                numberCell = new SliderCellDB();
                numberCell.setId(SliderCellDB.getUniqueId(realm));
                numberCell = realm.copyToRealm(numberCell);
                numberCell.setCell(cell.getDB());
                realm.commitTransaction();
            }

            ItemDB itemDB = numberCell.getItem();
            if(itemDB != null){
                item = itemDB.toGeneric();
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_cell_number_config, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        sprItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                OHItem item = items.get(position);
                if (item != null) {
                    realm.beginTransaction();
                    ItemDB itemDB = ItemDB.createOrLoadFromGeneric(realm, item);
                    if (item.getType().equals(OHItem.TYPE_NUMBER) || item.getType().equals(OHItem.TYPE_GROUP)) {
                        louRange.setVisibility(View.VISIBLE);
                    } else {
                        louRange.setVisibility(View.GONE);
                    }

                    numberCell.setItem(itemDB);
                    realm.commitTransaction();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mItemAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, items);
        sprItems.post(() -> sprItems.setAdapter(mItemAdapter));
        List<ServerDB> servers = realm.where(ServerDB.class).findAll();
        items.clear();

        if(item != null){
            items.add(item);
            mItemAdapter.add(item);
            mItemAdapter.notifyDataSetChanged();
        }

        for(final ServerDB serverDB : servers) {
            final OHServer server = serverDB.toGeneric();
            IServerHandler serverHandler = new Connector.ServerHandler(server, getContext());
            serverHandler.requestItemsRx()
                    .map(this::filterItems)
                    .compose(bindToLifecycle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(items -> {
                        this.items.addAll(items);
                        mItemAdapter.notifyDataSetChanged();
                    });
        }

        updateIconImage();
        btnSetIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), IconPickerActivity.class);
            startActivityForResult(intent, REQUEST_ICON);
        });

        if(numberCell != null){
            txtMax.setText(""+numberCell.getMax());
        }else{
            txtMax.setText(""+100);
        }

        return rootView;
    }

    private void updateIconImage(){
        btnSetIcon.setImageDrawable(Util.getIconDrawable(getActivity(), numberCell.getIcon()));
    }

    private List<OHItem> filterItems(List<OHItem> items){

        List<OHItem> tempItems = new ArrayList<>();
        for(OHItem item : items){
            if(item.getType().equals(OHItem.TYPE_NUMBER)){
                tempItems.add(item);
            }else if(item.getType().equals(OHItem.TYPE_DIMMER)){
                tempItems.add(item);
            }else if(item.getType().equals(OHItem.TYPE_COLOR)){
                tempItems.add(item);
            }else if(item.getType().equals(OHItem.TYPE_GROUP)){
                tempItems.add(item);
            }
        }
        items.clear();
        items.addAll(tempItems);

        return items;
    }

    @Override
    public void onPause() {
        super.onPause();

        if(numberCell.getItem() == null){
            return;
        }

        realm.beginTransaction();
        if(numberCell.getItem().getType().equals(OHItem.TYPE_NUMBER)
                || numberCell.getItem().getType().equals(OHItem.TYPE_GROUP)){
            numberCell.setMin(0);
            numberCell.setMax(Integer.parseInt(txtMax.getText().toString()));
        }else{
            numberCell.setMin(0);
            numberCell.setMax(100);
        }
        realm.commitTransaction();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ICON &&
                resultCode == Activity.RESULT_OK &&
                data.hasExtra(IconPickerActivity.RESULT_ICON)){

            String iconName = data.getStringExtra(IconPickerActivity.RESULT_ICON);
            realm.beginTransaction();
            numberCell.setIcon(iconName.equals("") ? null : iconName);
            realm.commitTransaction();
            updateIconImage();
        }
    }
}
