package treehou.se.habit.ui.sitemaps;

import android.app.Activity;
import android.content.Context;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;
import android.widget.TextView;

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
import se.treehou.ng.ohcommunicator.connector.models.OHLinkedPage;
import se.treehou.ng.ohcommunicator.connector.models.OHServer;
import se.treehou.ng.ohcommunicator.connector.models.OHSitemap;
import se.treehou.ng.ohcommunicator.connector.models.OHWidget;
import se.treehou.ng.ohcommunicator.services.IServerHandler;
import treehou.se.habit.DaggerActivityTestRule;
import treehou.se.habit.HabitApplication;
import treehou.se.habit.main.MainActivity;
import treehou.se.habit.NavigationUtil;
import treehou.se.habit.R;
import treehou.se.habit.data.TestAndroidModule;
import treehou.se.habit.data.TestConnectionFactory;
import treehou.se.habit.data.TestServerLoaderFactory;
import treehou.se.habit.module.ApplicationComponent;
import treehou.se.habit.module.DaggerApplicationComponent;
import treehou.se.habit.module.ServerLoaderFactory;
import treehou.se.habit.util.ConnectionFactory;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PageTest {

    static final String SERVER_NAME = "Test Server";
    static final String SITEMAP_NAME = "Test Sitemap";

    static final String PAGE_TITLE_1 = "Page Title 1";
    static final String PAGE_TITLE_2 = "Page Title 2";

    private OHLinkedPage linkedPageState1 = new OHLinkedPage();
    {
        List<OHWidget> widgets = new ArrayList<>();

        linkedPageState1.setTitle(SITEMAP_NAME);
        linkedPageState1.setId("");
        linkedPageState1.setLink("");
        linkedPageState1.setTitle(PAGE_TITLE_1);
        linkedPageState1.setWidgets(widgets);
    }

    private OHLinkedPage linkedPageState2 = new OHLinkedPage();
    {
        List<OHWidget> widgets = new ArrayList<>();

        linkedPageState2.setTitle(SITEMAP_NAME);
        linkedPageState2.setId("");
        linkedPageState2.setLink("");
        linkedPageState2.setTitle(PAGE_TITLE_2);
        linkedPageState2.setWidgets(widgets);
    }

    private BehaviorSubject<OHLinkedPage> linkedPageBehaviorSubject = BehaviorSubject.create(linkedPageState1);
    private OHServer server = new OHServer();

    @Rule
    public DaggerActivityTestRule<MainActivity> activityRule = new DaggerActivityTestRule<MainActivity>(MainActivity.class){

        @Override
        public ApplicationComponent setupComponent(HabitApplication application, Activity activity) {
            return createComponent(application);
        }
    };

    @Test
    public void testDisplaySitemaps() {
        NavigationUtil.navigateToSitemap();
        onView(allOf(isDescendantOfA(withId(R.id.list)), withText(SITEMAP_NAME))).perform(ViewActions.click());
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.toolbar))))
                .check(matches(withText(linkedPageState1.getTitle())));
        linkedPageBehaviorSubject.onNext(linkedPageState2);
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.toolbar))))
                .check(matches(withText(linkedPageState2.getTitle())));
    }

    private ApplicationComponent createComponent(HabitApplication application){
        ApplicationComponent component = DaggerApplicationComponent.builder()
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
                            public Observable.Transformer<OHServer, ServerSitemapsResponse> serverToSitemap(Context context) {

                                OHSitemap sitemap = new OHSitemap();
                                sitemap.setName(SITEMAP_NAME);
                                sitemap.setServer(server);

                                List<OHSitemap> sitemapList = new ArrayList<>();
                                sitemapList.add(sitemap);

                                return observable -> observable.flatMap((Func1<OHServer, Observable<List<OHSitemap>>>) server -> {
                                    return Observable.just(sitemapList);
                                }, (server, sitemaps) -> {
                                    sitemap.setServer(server);
                                    return new ServerSitemapsResponse(server, sitemaps);
                                });
                            }
                        };
                    }

                    @Override
                    public ConnectionFactory provideConnectionFactory() {
                        return new TestConnectionFactory(application){
                            @Override
                            public IServerHandler createServerHandler(OHServer server, Context context) {
                                return new TestServerHandler(){

                                    @Override
                                    public Observable<List<OHSitemap>> requestSitemapRx() {
                                        OHSitemap sitemap = new OHSitemap();
                                        sitemap.setName(SITEMAP_NAME);
                                        sitemap.setServer(server);

                                        List<OHSitemap> sitemaps = new ArrayList<>();
                                        sitemaps.add(sitemap);

                                        return Observable.just(sitemaps);
                                    }

                                    @Override
                                    public Observable<OHLinkedPage> requestPageUpdatesRx(OHLinkedPage ohLinkedPage) {
                                        return linkedPageBehaviorSubject.asObservable();
                                    }

                                    @Override
                                    public Observable<OHLinkedPage> requestPageRx(OHLinkedPage ohLinkedPage) {
                                        return linkedPageBehaviorSubject.asObservable();
                                    }
                                };
                            }
                        };
                    }
                }).build();

        return component;
    }
}