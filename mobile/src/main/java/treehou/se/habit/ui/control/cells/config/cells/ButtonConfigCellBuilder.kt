package treehou.se.habit.ui.control.cells.config.cells


import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RemoteViews
import butterknife.BindView
import butterknife.ButterKnife
import io.realm.Realm
import treehou.se.habit.R
import treehou.se.habit.core.db.model.controller.CellDB
import treehou.se.habit.core.db.model.controller.ControllerDB
import treehou.se.habit.ui.control.CellFactory
import treehou.se.habit.ui.control.ControllerUtil
import treehou.se.habit.util.Util

class ButtonConfigCellBuilder : CellFactory.CellBuilder {

    @BindView(R.id.img_icon_button) lateinit var imgIcon: ImageButton

    override fun build(context: Context, controller: ControllerDB, cell: CellDB): View {
        val inflater = LayoutInflater.from(context)
        val cellView = inflater.inflate(R.layout.cell_conf_button, null)
        ButterKnife.bind(this, cellView)

        val realm = Realm.getDefaultInstance()
        val buttonCell = cell.getCellButton()

        val pallete = ControllerUtil.generateColor(controller, cell)

        cellView.setBackgroundColor(pallete[ControllerUtil.INDEX_BUTTON])

        imgIcon.background.setColorFilter(pallete[ControllerUtil.INDEX_BUTTON], PorterDuff.Mode.MULTIPLY)

        Log.d(TAG, "Build: Button icon " + buttonCell!!.icon)

        val icon = Util.getIconDrawable(context, buttonCell.icon)
        if (icon != null) {
            imgIcon.setImageDrawable(icon)
        }
        realm.close()

        return cellView
    }


    override fun buildRemote(context: Context, controller: ControllerDB, cell: CellDB): RemoteViews? {
        return null
    }

    companion object {

        private val TAG = "ButtonConfigCellBuilder"
    }
}
