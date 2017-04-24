package treehou.se.habit.module;


import dagger.Binds;
import dagger.Module;
import dagger.android.ActivityKey;
import dagger.android.support.FragmentKey;
import dagger.multibindings.IntoMap;
import treehou.se.habit.main.MainActivity;
import treehou.se.habit.main.MainActivityComponent;
import treehou.se.habit.ui.sitemaps.PageComponent;
import treehou.se.habit.ui.sitemaps.PageFragment;
import treehou.se.habit.ui.sitemaps.sitemap.SitemapComponent;
import treehou.se.habit.ui.sitemaps.sitemap.SitemapFragment;
import treehou.se.habit.ui.sitemaps.sitemaplist.SitemapListComponent;
import treehou.se.habit.ui.settings.SettingsComponent;
import treehou.se.habit.ui.settings.SettingsFragment;
import treehou.se.habit.ui.settings.subsettings.general.GeneralSettingsComponent;
import treehou.se.habit.ui.settings.subsettings.general.GeneralSettingsFragment;
import treehou.se.habit.ui.settings.subsettings.wiget.WidgetSettingsComponent;
import treehou.se.habit.ui.settings.subsettings.wiget.WidgetSettingsFragment;
import treehou.se.habit.ui.sitemaps.sitemaplist.SitemapListFragment;

@Module(
subcomponents = {
        MainActivityComponent.class,
        SettingsComponent.class,
        GeneralSettingsComponent.class,
        WidgetSettingsComponent.class,
        SitemapComponent.class,
        PageComponent.class,
        SitemapListComponent.class
})
public abstract class AppBindingModule {

    @Binds
    @IntoMap
    @ActivityKey(MainActivity.class)
    public abstract ActivityComponentBuilder mainActivityComponentBuilder(MainActivityComponent.Builder impl);

    @Binds
    @IntoMap
    @FragmentKey(SitemapListFragment.class)
    public abstract FragmentComponentBuilder sitemapListComponentBuilder(SitemapListComponent.Builder impl);

    @Binds
    @IntoMap
    @FragmentKey(SitemapFragment.class)
    public abstract FragmentComponentBuilder sitemapComponentBuilder(SitemapComponent.Builder impl);

    @Binds
    @IntoMap
    @FragmentKey(SettingsFragment.class)
    public abstract FragmentComponentBuilder settingsComponentBuilder(SettingsComponent.Builder impl);

    @Binds
    @IntoMap
    @FragmentKey(GeneralSettingsFragment.class)
    public abstract FragmentComponentBuilder generalSettingsComponentBuilder(GeneralSettingsComponent.Builder impl);

    @Binds
    @IntoMap
    @FragmentKey(PageFragment.class)
    public abstract FragmentComponentBuilder pageComponentBuilder(PageComponent.Builder impl);

    @Binds
    @IntoMap
    @FragmentKey(WidgetSettingsFragment.class)
    public abstract FragmentComponentBuilder widgetSettingsComponentBuilder(WidgetSettingsComponent.Builder impl);
}
