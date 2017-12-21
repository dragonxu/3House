package treehou.se.habit.ui.homescreen;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import treehou.se.habit.BaseActivity;
import treehou.se.habit.HabitApplication;
import treehou.se.habit.R;
import treehou.se.habit.core.db.model.controller.ControllerDB;
import treehou.se.habit.ui.control.ControllerUtil;

/**
 * The configuration screen for the {@link ControllerWidget ControllerWidget} AppWidget.
 */
public class ControllerWidgetConfigureActivity extends BaseActivity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_NAME = "treehou.se.habit.ui.homescreen.ControllerWidget";
    private static final String PREF_PREFIX_KEY             = "appwidget_";
    private static final String PREF_POSTFIX_SHOW_TITLE     = "_show_title";

    @BindView(R.id.spr_controller) Spinner sprControllers;
    @BindView(R.id.cbx_show_title) CheckBox cbxShowTitle;
    @BindView(R.id.add_button) Button addButton;

    @Inject ControllerUtil controllerUtil;

    private Unbinder unbinder;

    public ControllerWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        ((HabitApplication)getApplicationContext()).component().inject(this);
        super.onCreate(icicle);

        setContentView(R.layout.controller_widget_configure);

        unbinder = ButterKnife.bind(this);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        List<ControllerDB> controllers = getRealm().where(ControllerDB.class).findAll();
        List<ControllerItem> controllerItems = new ArrayList<>();
        for(ControllerDB controllerDB : controllers) controllerItems.add(new ControllerItem(controllerDB));
        
        ArrayAdapter<ControllerItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, controllerItems);
        sprControllers.setAdapter(adapter);

        RxAdapterView.itemSelections(sprControllers)
                .map(index -> index != AdapterView.INVALID_POSITION)
                .subscribe(RxView.enabled(addButton));

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this view was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    class ControllerItem{
        private ControllerDB controllerDB;

        public ControllerItem(ControllerDB controllerDB) {
            this.controllerDB = controllerDB;
        }

        public ControllerDB getControllerDB() {
            return controllerDB;
        }

        @Override
        public String toString() {
            return controllerDB.getName();
        }
    }

    @OnClick(R.id.add_button)
    public void onAddClick() {
        final Context context = ControllerWidgetConfigureActivity.this;

        ControllerDB controller = ((ControllerItem) sprControllers.getSelectedItem()).getControllerDB();
        if(controller == null){
            Toast.makeText(ControllerWidgetConfigureActivity.this, getString(R.string.failed_save_controller), Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        saveControllerIdPref(context, mAppWidgetId, controller, cbxShowTitle.isChecked());

        // It is the responsibility of the configuration view to update the app widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        controllerUtil.updateWidget(mAppWidgetId);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveControllerIdPref(Context context, int appWidgetId, ControllerDB controller, boolean showTitle) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_PREFIX_KEY + appWidgetId, controller.getId());
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_POSTFIX_SHOW_TITLE, showTitle);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static long loadControllIdPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1);
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static boolean loadControllShowTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_POSTFIX_SHOW_TITLE, true);
    }

    static void deletePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + PREF_POSTFIX_SHOW_TITLE);
        prefs.apply();
    }
}



