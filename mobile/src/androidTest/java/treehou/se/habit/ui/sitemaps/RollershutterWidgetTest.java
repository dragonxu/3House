package treehou.se.habit.ui.sitemaps;

import android.content.Context;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;
import android.test.RenamingDelegatingContext;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import se.treehou.ng.ohcommunicator.connector.models.OHItem;
import se.treehou.ng.ohcommunicator.connector.models.OHLinkedPage;
import se.treehou.ng.ohcommunicator.connector.models.OHServer;
import se.treehou.ng.ohcommunicator.connector.models.OHSitemap;
import se.treehou.ng.ohcommunicator.connector.models.OHStateDescription;
import se.treehou.ng.ohcommunicator.connector.models.OHWidget;
import se.treehou.ng.ohcommunicator.services.IServerHandler;
import se.treehou.ng.ohcommunicator.util.OpenhabConstants;
import treehou.se.habit.DaggerActivityTestRule;
import treehou.se.habit.DaggerHabitApplication_ApplicationComponent;
import treehou.se.habit.DatabaseUtil;
import treehou.se.habit.HabitApplication;
import treehou.se.habit.MainActivity;
import treehou.se.habit.NavigationUtil;
import treehou.se.habit.R;
import treehou.se.habit.connector.Constants;
import treehou.se.habit.data.TestAndroidModule;
import treehou.se.habit.data.TestConnectionFactory;
import treehou.se.habit.data.TestServerLoaderFactory;
import treehou.se.habit.module.ServerLoaderFactory;
import treehou.se.habit.util.ConnectionFactory;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RollershutterWidgetTest {

    static final String SERVER_NAME = "Test Server";
    static final String SITEMAP_NAME = "Test Sitemap";
    static final String WIDGET_LABEL = "Widget test";
    static final String WIDGET_ITEM = "Item test";

    private OHLinkedPage linkedPageState1 = new OHLinkedPage();
    {
        OHItem item = new OHItem();
        item.setType(OHItem.TYPE_ROLLERSHUTTER);
        item.setState(Constants.COMMAND_OFF);
        item.setName(WIDGET_ITEM);
        item.setLink("");

        OHStateDescription stateDescription = new OHStateDescription();
        stateDescription.setReadOnly(false);
        item.setStateDescription(stateDescription);

        OHWidget testWidget = new OHWidget();
        testWidget.setLabel(WIDGET_LABEL);
        testWidget.setIcon("");
        testWidget.setType(OpenhabConstants.TYPE_SWITCH);
        testWidget.setWidgetId("");
        testWidget.setItem(item);

        List<OHWidget> widgets = new ArrayList<>();
        widgets.add(testWidget);

        linkedPageState1.setTitle(SITEMAP_NAME);
        linkedPageState1.setId("");
        linkedPageState1.setLink("");
        linkedPageState1.setTitle("");
        linkedPageState1.setWidgets(widgets);
    }

    private BehaviorSubject<OHLinkedPage> linkedPageBehaviorSubject = BehaviorSubject.create(linkedPageState1);
    private OHServer server = new OHServer();

    @Rule
    public DaggerActivityTestRule<MainActivity> activityRule = new DaggerActivityTestRule<>(MainActivity.class, (application, activity) -> {

        HabitApplication.ApplicationComponent component = DaggerHabitApplication_ApplicationComponent.builder()
                .androidModule(new TestAndroidModule(application){

                    @Override
                    public ServerLoaderFactory provideServerLoaderFactory(ConnectionFactory connectionFactory) {

                        server.setName(SERVER_NAME);

                        return new TestServerLoaderFactory(connectionFactory) {

                            @Override
                            public OHServer loadServer(Realm realm, long id) {
                                return server;
                            }

                            @Override
                            public Observable.Transformer<Realm, OHServer> loadServersRx() {
                                return observable -> observable.flatMap(realmLocal -> Observable.just(server));
                            }

                            @Override
                            public Observable.Transformer<OHServer, Pair<OHServer, List<OHSitemap>>> serverToSitemap(Context context) {

                                OHSitemap sitemap = new OHSitemap();
                                sitemap.setName(SITEMAP_NAME);
                                sitemap.setServer(server);

                                List<OHSitemap> sitemapList = new ArrayList<>();
                                sitemapList.add(sitemap);

                                return observable -> observable.flatMap(new Func1<OHServer, Observable<List<OHSitemap>>>() {
                                    @Override
                                    public Observable<List<OHSitemap>> call(OHServer server) {
                                        return Observable.just(sitemapList);
                                    }
                                }, (server, sitemaps) -> {
                                    sitemap.setServer(server);
                                    return new Pair<>(server, sitemaps);
                                });
                            }
                        };
                    }

                    @Override
                    public ConnectionFactory provideConnectionFactory() {
                        return new TestConnectionFactory(){
                            @Override
                            public IServerHandler createServerHandler(OHServer server, Context context) {
                                return new TestServerHandler(){

                                    @Override
                                    public Observable<List<OHSitemap>> requestSitemapObservable() {
                                        OHSitemap sitemap = new OHSitemap();
                                        sitemap.setName(SITEMAP_NAME);
                                        sitemap.setServer(server);

                                        List<OHSitemap> sitemaps = new ArrayList<>();
                                        sitemaps.add(sitemap);

                                        return Observable.just(sitemaps);
                                    }

                                    @Override
                                    public Observable<OHLinkedPage> requestPageUpdatesRx(OHServer ohServer, OHLinkedPage ohLinkedPage) {
                                        return linkedPageBehaviorSubject.asObservable();
                                    }

                                    @Override
                                    public Observable<OHLinkedPage> requestPageRx(OHLinkedPage ohLinkedPage) {
                                        return linkedPageBehaviorSubject.asObservable().first();
                                    }
                                };
                            }
                        };
                    }
                }).build();

        ((HabitApplication) application).setTestComponent(component);
        Context renamedContext = new RenamingDelegatingContext(application, "Testus");
        DatabaseUtil.init(renamedContext);
    });

    @Test
    public void testDisplaySitemaps() {
        NavigationUtil.navigateToSitemap();
        onView(withText(SITEMAP_NAME)).perform(ViewActions.click());
        onView(withText(WIDGET_LABEL)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_up)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_down)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_cancel)).check(matches(isDisplayed()));
    }
}