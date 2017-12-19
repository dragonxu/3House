package treehou.se.habit.ui.control.cells.config;

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

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import se.treehou.ng.ohcommunicator.connector.models.OHItem;
import se.treehou.ng.ohcommunicator.connector.models.OHServer;
import se.treehou.ng.ohcommunicator.services.IServerHandler;
import treehou.se.habit.R;
import treehou.se.habit.core.db.model.ItemDB;
import treehou.se.habit.core.db.model.ServerDB;
import treehou.se.habit.core.db.model.controller.CellDB;
import treehou.se.habit.core.db.model.controller.SliderCellDB;
import treehou.se.habit.ui.BaseFragment;
import treehou.se.habit.util.ConnectionFactory;
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

    @Inject ConnectionFactory connectionFactory;

    private ArrayAdapter<OHItem> mItemAdapter ;
    private ArrayList<OHItem> items = new ArrayList<>();
    private SliderCellDB sliderCell;
    private CellDB cell;
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

        Util.getApplicationComponent(this).inject(this);

        if (getArguments() != null) {
            long id = getArguments().getLong(ARG_CELL_ID);
            cell = CellDB.load(getRealm(), id);
            sliderCell = cell.getCellSlider();

            if(sliderCell == null){
                getRealm().executeTransaction(realm -> {
                    sliderCell = new SliderCellDB();
                    sliderCell = realm.copyToRealm(sliderCell);
                    cell.setCellSlider(sliderCell);
                    realm.copyToRealmOrUpdate(cell);
                });
            }

            ItemDB itemDB = sliderCell.getItem();
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
                    getRealm().beginTransaction();
                    ItemDB itemDB = ItemDB.createOrLoadFromGeneric(getRealm(), item);
                    if (item.getType().equals(OHItem.TYPE_NUMBER) || item.getType().equals(OHItem.TYPE_GROUP)) {
                        louRange.setVisibility(View.VISIBLE);
                    } else {
                        louRange.setVisibility(View.GONE);
                    }

                    sliderCell.setItem(itemDB);
                    getRealm().commitTransaction();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mItemAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, items);
        sprItems.post(() -> sprItems.setAdapter(mItemAdapter));
        List<ServerDB> servers = getRealm().where(ServerDB.class).findAll();
        items.clear();

        if(item != null){
            items.add(item);
            mItemAdapter.add(item);
            mItemAdapter.notifyDataSetChanged();
        }

        for(final ServerDB serverDB : servers) {
            final OHServer server = serverDB.toGeneric();
            IServerHandler serverHandler = connectionFactory.createServerHandler(server, getContext());
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

        if(sliderCell != null){
            txtMax.setText(""+ sliderCell.getMax());
        }else{
            txtMax.setText(""+100);
        }

        return rootView;
    }

    private void updateIconImage(){
        btnSetIcon.setImageDrawable(Util.getIconDrawable(getActivity(), sliderCell.getIcon()));
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

        if(sliderCell.getItem() == null){
            return;
        }

        getRealm().beginTransaction();
        if(sliderCell.getItem().getType().equals(OHItem.TYPE_NUMBER)
                || sliderCell.getItem().getType().equals(OHItem.TYPE_GROUP)){
            sliderCell.setMin(0);
            sliderCell.setMax(Integer.parseInt(txtMax.getText().toString()));
        }else{
            sliderCell.setMin(0);
            sliderCell.setMax(100);
        }
        getRealm().commitTransaction();
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
            getRealm().beginTransaction();
            sliderCell.setIcon(iconName.equals("") ? null : iconName);
            getRealm().commitTransaction();
            updateIconImage();
        }
    }
}
