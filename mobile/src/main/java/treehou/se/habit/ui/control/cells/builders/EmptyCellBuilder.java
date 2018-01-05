package treehou.se.habit.ui.control.cells.builders;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RemoteViews;

import io.realm.Realm;
import treehou.se.habit.R;
import treehou.se.habit.core.db.model.controller.ButtonCellDB;
import treehou.se.habit.core.db.model.controller.CellDB;
import treehou.se.habit.core.db.model.controller.ControllerDB;
import treehou.se.habit.ui.util.ViewHelper;
import treehou.se.habit.ui.control.CellFactory;
import treehou.se.habit.ui.control.ControllerUtil;

public class EmptyCellBuilder implements CellFactory.CellBuilder {

    public View build(Context context, ControllerDB controller, CellDB cell){
        LayoutInflater inflater = LayoutInflater.from(context);
        View rootView = inflater.inflate(R.layout.cell_empty, null);

        int[] pallete = ControllerUtil.generateColor(controller, cell);

        rootView.setBackgroundColor(pallete[ControllerUtil.Companion.getINDEX_BUTTON()]);
        ImageButton imgButton = (ImageButton) rootView.findViewById(R.id.img_icon_button);
        imgButton.getBackground().setColorFilter(pallete[ControllerUtil.Companion.getINDEX_BUTTON()], PorterDuff.Mode.MULTIPLY);

        return rootView;
    }

    @Override
    public RemoteViews buildRemote(Context context, ControllerDB controller, CellDB cell) {
        Realm realm = Realm.getDefaultInstance();
        final ButtonCellDB buttonCell = cell.getCellButton();

        RemoteViews cellView = new RemoteViews(context.getPackageName(), R.layout.cell_empty);
        int[] pallete = ControllerUtil.generateColor(controller, cell);
        ViewHelper.colorRemoteDrawable(cellView, R.id.img_icon_button, pallete[ControllerUtil.Companion.getINDEX_BUTTON()]);
        realm.close();

        return cellView;
    }
}
