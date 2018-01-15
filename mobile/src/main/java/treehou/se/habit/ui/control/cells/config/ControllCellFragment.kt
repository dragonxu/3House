package treehou.se.habit.ui.control.cells.config

import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import io.realm.Realm
import treehou.se.habit.R
import treehou.se.habit.core.db.model.controller.CellDB
import treehou.se.habit.ui.colorpicker.ColorDialog

class ControllCellFragment : Fragment(), ColorDialog.ColorDialogCallback {

    @BindView(R.id.btn_color_picker) lateinit var btnPicker: Button
    @BindView(R.id.spr_items) lateinit var sprItems: Spinner
    private var mTypeAdapter: ArrayAdapter<*>? = null
    private var cell: CellDB? = null

    private var realm: Realm? = null
    private var unbinder: Unbinder? = null

    private val itemSelectListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

            val cellType = resources.getIntArray(R.array.cell_types_values)[position]

            Log.d(TAG, "item selected $cellType $position")

            val fragmentManager = activity!!.supportFragmentManager
            var fragment: Fragment? = null
            val cell = cell
            if(cell != null) {
                when (cellType) {
                    CellDB.TYPE_BUTTON -> {
                        Log.d(TAG, "Loading button fragment.")
                        fragment = CellButtonConfigFragment.newInstance(cell)
                    }
                    CellDB.TYPE_SLIDER -> {
                        Log.d(TAG, "Loading slider fragment.")
                        fragment = CellSliderConfigFragment.newInstance(cell)
                    }
                    CellDB.TYPE_VOICE -> {
                        Log.d(TAG, "Loading voice fragment.")
                        fragment = CellVoiceConfigFragment.newInstance(cell)
                    }
                    CellDB.TYPE_INC_DEC -> {
                        Log.d(TAG, "Loading IncDec fragment.")
                        fragment = CellIncDecConfigFragment.newInstance(cell)
                    }
                    else -> {
                        Log.d(TAG, "Loading empty fragment.")
                        val currentFragment = fragmentManager.findFragmentById(R.id.lou_config_container)
                        if (currentFragment != null) {
                            fragmentManager.beginTransaction()
                                    .remove(currentFragment)
                                    .commit()
                        }
                    }
                }
            }

            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .replace(R.id.lou_config_container, fragment)
                        .commit()
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()
        if (arguments != null) {
            val cellId = arguments!!.getLong(ARG_CELL_ID)
            cell = CellDB.load(realm, cellId)
        }

        val cellTypes = resources.getStringArray(R.array.cell_types)
        mTypeAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_dropdown_item, cellTypes)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_controll_cell, container, false)
        unbinder = ButterKnife.bind(this, rootView)

        sprItems.adapter = mTypeAdapter
        sprItems.onItemSelectedListener = itemSelectListener

        updateColorButton(cell!!.color)
        btnPicker.setOnClickListener {
            val dialog = ColorDialog.instance()
            dialog.setTargetFragment(this@ControllCellFragment, REQUEST_COLOR)
            activity!!.supportFragmentManager.beginTransaction()
                    .add(dialog, "colordialog")
                    .commit()
        }
        Log.d(TAG, "Color is : " + cell!!.color)

        val typeArray = resources.getIntArray(R.array.cell_types_values)
        var index = 0
        for (i in typeArray.indices) {
            if (typeArray[i] == cell!!.type) {
                index = i
                break
            }
        }
        sprItems.setSelection(index)

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()

        realm!!.close()
    }

    /**
     * Update the color of color button
     * @param color the color to set
     */
    fun updateColorButton(@ColorInt color: Int) {
        btnPicker.setBackgroundColor(color)
    }

    override fun setColor(color: Int) {
        Log.d(TAG, "Color set: " + color)
        updateColorButton(color)

        realm!!.beginTransaction()
        cell!!.color = color
        realm!!.commitTransaction()
    }

    companion object {

        val TAG = "ControllCellFragment"
        val ARG_CELL_ID = "ARG_CELL_ID"
        val REQUEST_COLOR = 3001

        fun newInstance(cellId: Long): ControllCellFragment {
            val fragment = ControllCellFragment()
            val args = Bundle()
            args.putLong(ARG_CELL_ID, cellId)
            fragment.arguments = args

            return fragment
        }
    }
}
