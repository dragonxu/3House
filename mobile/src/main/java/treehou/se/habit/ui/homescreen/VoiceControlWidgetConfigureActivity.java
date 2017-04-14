package treehou.se.habit.ui.homescreen;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.realm.Realm;
import io.realm.RealmResults;
import treehou.se.habit.R;
import treehou.se.habit.core.db.model.ServerDB;
import treehou.se.habit.ui.adapter.ServerArrayAdapter;

/**
 * The configuration screen for the {@link VoiceControlWidget VoiceControlWidget} AppWidget.
 */
public class VoiceControlWidgetConfigureActivity extends Activity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_SERVER = "treehou.se.habit.ui.homescreen.VoiceControlWidget";
    private static final String PREF_POSTFIX_SHOW_TITLE     = "_show_title";
    private static final String PREF_PREFIX_KEY = "appwidget_voice_";

    @BindView(R.id.spr_server) Spinner sprServers;
    @BindView(R.id.cbx_show_title) CheckBox cbxShowTitle;
    private Unbinder unbinder;

    public VoiceControlWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.voice_control_widget_configure);
        unbinder = ButterKnife.bind(this);

        Realm realm = Realm.getDefaultInstance();
        RealmResults<ServerDB> servers = realm.where(ServerDB.class).findAll();
        realm.close();

        ArrayAdapter<ServerDB> serverAdapter = new ServerArrayAdapter(this, servers);
        sprServers.setAdapter(serverAdapter);

        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

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

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = VoiceControlWidgetConfigureActivity.this;

            // It is the responsibility of the configuration view to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            //TODO check that server is selected
            saveServerPref(VoiceControlWidgetConfigureActivity.this, mAppWidgetId, (ServerDB)sprServers.getSelectedItem(), cbxShowTitle.isChecked());

            VoiceControlWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    // Write the prefix to the SharedPreferences object for this widget
    static void saveServerPref(Context context, int appWidgetId, ServerDB server, boolean showTitle) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_SERVER, 0).edit();
        prefs.putLong(PREF_PREFIX_KEY + appWidgetId, server.getId());
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_POSTFIX_SHOW_TITLE, showTitle);
        prefs.apply();
    }

    static boolean loadControllShowTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SERVER, 0);
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_POSTFIX_SHOW_TITLE, true);
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static long loadServerPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SERVER, 0);
        return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1);
    }


    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_SERVER, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

}



