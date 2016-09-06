package treehou.se.habit.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.jakewharton.rxbinding.widget.RxCompoundButton;

import javax.inject.Inject;

import rx.Observable;
import se.treehou.ng.ohcommunicator.connector.models.OHSitemap;
import treehou.se.habit.R;

public class Settings {

    private static final String PREF_MANAGER = "treePref";

    private static final String PREF_DEFAULT_SITEMAP = "default_sitemap_name";
    private static final String PREF_THEME = "pref_them";
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    private static final String PREF_SERVER_SETUP_QUESTION = "pref_server_setup_question";
    private static final String PREF_AUTOLOAD_SITEMAP = "pref_autoload_sitemap";
    private static final String PREF_SHOW_SITEMAPS_IN_MENU = "pref_show_sitemap_in_menu";

    private static final int DEFAULT_THEME = R.style.AppTheme_Base;

    @Inject SharedPreferences preferences;
    RxSharedPreferences rxPreferences;

    private Preference<Integer> prefTheme;
    private Preference<Boolean> prefSitemapInMenu;

    public Settings(Context context) {
        preferences = context.getSharedPreferences(PREF_MANAGER, Context.MODE_PRIVATE);
        rxPreferences = RxSharedPreferences.create(preferences);
        prefTheme = rxPreferences.getInteger(PREF_THEME, DEFAULT_THEME);
        prefSitemapInMenu = rxPreferences.getBoolean(PREF_SHOW_SITEMAPS_IN_MENU, true);
    }

    public static Settings instance(Context context){
        return new Settings(context);
    }

    /**
     * The user manually opened the drawer; store this flag to prevent auto-showing
     * the navigation drawer automatically in the future.
     *
     * @param learned true if user interacted with drawer, else false.
     */
    public void userLearnedDrawer(boolean learned) {
        preferences.edit().putBoolean(PREF_USER_LEARNED_DRAWER, learned).apply();
    }

    /**
     * Get the sitemap to open by default
     *
     * @return default sitemap.
     */
    public String getDefaultSitemap(){
        return preferences.getString(PREF_DEFAULT_SITEMAP, "");
    }

    /**
     * Get theme of application
     * @return application theme.
     */
    public int getTheme(){
        return prefTheme.get();
    }

    /**
     * Get theme of application
     * @return application theme.
     */
    public Observable<Integer> getThemeRx(){
        return prefTheme.asObservable().distinctUntilChanged();
    }

    /**
     * Set application theme.
     *
     * @param theme application theme
     */
    public void setTheme(int theme){
        prefTheme.set(theme);
    }

    /**
     * Sets the sitemap to be used when logging in
     *
     * @param sitemap new default sitemap
     */
    public void setDefaultSitemap(OHSitemap sitemap){
        SharedPreferences.Editor editor = preferences.edit();
        if(sitemap != null) {
            editor.putString(PREF_DEFAULT_SITEMAP, sitemap.getName());
        } else {
            editor.remove(PREF_DEFAULT_SITEMAP);
        }
        editor.apply();
    }

    /**
     * Returns observable returning a stream of anwets if we should show sitemaps in menu.
     * @return observable emitting true if sitemaps should be shown, else false.
     */
    public Preference<Boolean> getShowSitemapsInMenuRx() {
         return prefSitemapInMenu;
    }

    /**
     * Set if sitemap should be shown in menu.
     * @param value true to show in menu, else false.
     */
    public void setShowSitemapsInMenu(boolean value) {
        prefSitemapInMenu.set(value);
    }

    /**
     * Check if sitemap should be autoloaded when sitemap list starts up.
     * @return get preference as rx pref.
     */
    public Preference<Boolean> getAutoloadSitemapRx() {
        return rxPreferences.getBoolean(PREF_AUTOLOAD_SITEMAP, true);
    }

    /**
     * Set if app has asked user to setup server.
     * @param isAsked true if question has been asked, else false.
     */
    public void setServerSetupAsked(boolean isAsked){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_SERVER_SETUP_QUESTION, isAsked);
        editor.apply();
    }

    /**
     * Check if app has asked user to setup initial server.
     * @return true if no question has been asked. else false.
     */
    public boolean getServerSetupAsked(){
        return preferences.getBoolean(PREF_SERVER_SETUP_QUESTION, false);
    }
}
