package treehou.se.habit.ui.control.cells.builders

import android.app.PendingIntent
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
import treehou.se.habit.ui.control.CommandService
import treehou.se.habit.ui.control.ControllerUtil
import treehou.se.habit.ui.util.ViewHelper
import treehou.se.habit.util.ConnectionFactory
import treehou.se.habit.util.Util

class ButtonCellBuilder(private val connectionFactory: ConnectionFactory) : CellFactory.CellBuilder {

    @BindView(R.id.img_icon_button) lateinit var imgIcon: ImageButton

    override fun build(context: Context, controller: ControllerDB, cell: CellDB): View {
        val inflater = LayoutInflater.from(context)
        val cellView = inflater.inflate(R.layout.cell_button, null)
        ButterKnife.bind(this, cellView)

        Log.d(TAG, "Build: Button")
        val realm = Realm.getDefaultInstance()
        val buttonCell = cell.cellButton

        val pallete = ControllerUtil.generateColor(controller, cell)
        cellView.setBackgroundColor(pallete[ControllerUtil.INDEX_BUTTON])

        imgIcon.background.setColorFilter(pallete[ControllerUtil.INDEX_BUTTON], PorterDuff.Mode.MULTIPLY)

        Log.d(TAG, "Build: Button icon " + buttonCell.icon)

        imgIcon.setImageDrawable(Util.getIconDrawable(context, buttonCell.icon))

        imgIcon.setOnClickListener {
            val item = buttonCell.item
            if (item != null) {
                val server = item.server.toGeneric()
                val serverHandler = connectionFactory.createServerHandler(server, context)
                serverHandler.sendCommand(item.name, buttonCell.command)
            }
        }
        realm.close()

        return cellView
    }


    override fun buildRemote(context: Context, controller: ControllerDB, cell: CellDB): RemoteViews? {

        val realm = Realm.getDefaultInstance()
        val buttonCell = cell.cellButton

        val cellView = RemoteViews(context.packageName, R.layout.cell_button)

        val pallete = ControllerUtil.generateColor(controller, cell)
        ViewHelper.colorRemoteDrawable(cellView, R.id.img_icon_button, pallete[ControllerUtil.INDEX_BUTTON])

        cellView.setImageViewBitmap(R.id.img_icon_button, Util.getIconBitmap(context, buttonCell.icon))
        val intent = CommandService.getActionCommand(context, buttonCell.command, buttonCell.item.id)

        //TODO give intent unique id
        val pendingIntent = PendingIntent.getService(context, (Math.random() * Integer.MAX_VALUE).toInt(), intent, PendingIntent.FLAG_CANCEL_CURRENT)
        cellView.setOnClickPendingIntent(R.id.img_icon_button, pendingIntent)
        realm.close()

        return cellView
    }

    companion object {

        private val TAG = "ButtonCellBuilder"
    }
}
