package treehou.se.habit.ui.sitemaps.page

import android.content.Context
import android.os.Build
import android.os.Bundle

import com.google.gson.Gson

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Named

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import se.treehou.ng.ohcommunicator.connector.models.OHLinkedPage
import se.treehou.ng.ohcommunicator.connector.models.OHServer
import se.treehou.ng.ohcommunicator.connector.models.OHWidget
import se.treehou.ng.ohcommunicator.services.IServerHandler
import se.treehou.ng.ohcommunicator.util.GsonHelper
import treehou.se.habit.core.db.model.ServerDB
import treehou.se.habit.module.RxPresenter
import treehou.se.habit.module.ServerLoaderFactory
import treehou.se.habit.ui.widgets.WidgetFactory
import treehou.se.habit.util.ConnectionFactory
import treehou.se.habit.util.RxUtil
import treehou.se.habit.util.logging.Logger

class PagePresenter @Inject
constructor(private val view: PageContract.View, private val fragment: PageFragment, private val context: Context, @param:Named("arguments") private val args: Bundle, private val log: Logger, private val widgetFactory: WidgetFactory, private val serverLoaderFactory: ServerLoaderFactory, private val connectionFactory: ConnectionFactory, private val realm: Realm) : RxPresenter(), PageContract.Presenter {

    private val widgets = ArrayList<OHWidget>()
    private val widgetHolders = ArrayList<WidgetFactory.IWidgetHolder>()
    private var initialized = false

    private var server: ServerDB? = null
    private var page: OHLinkedPage? = null


    private val dataLoadError = Consumer<Throwable> { throwable ->
        log.e(TAG, "Error when requesting page ", throwable)
        view.showLostServerConnectionMessage()
        view.closeView()
    }

    override fun load(launchData: Bundle?, savedData: Bundle?) {
        super.load(launchData, savedData)
        val gson = GsonHelper.createGsonBuilder()

        val serverId = args.getLong(PageContract.ARG_SERVER)
        var jPage = args.getString(PageContract.ARG_PAGE)

        server = ServerDB.load(realm, serverId)
        page = gson.fromJson(jPage, OHLinkedPage::class.java)

        initialized = false
        if (savedData != null && savedData.containsKey(PageContract.STATE_PAGE)) {
            jPage = savedData.getString(PageContract.STATE_PAGE)
            val savedPage = gson.fromJson(jPage, OHLinkedPage::class.java)
            if (savedPage.id == page!!.id) {
                page = savedPage
                initialized = true
            }
        }
    }


    override fun subscribe() {
        super.subscribe()
        updatePage(page, true)
        if (!initialized && server != null) {
            requestPageUpdate()
        }
        initialized = true

        // Start listening for server updates
        if (supportsLongPolling()) {
            createLongPoller()
        }
    }

    override fun save(savedData: Bundle?) {
        super.save(savedData)
        savedData!!.putSerializable(PageContract.STATE_PAGE, GsonHelper.createGsonBuilder().toJson(page))
    }

    /**
     * Check if android device supports long polling.
     * @return true if long polling is supported, else false.
     */
    private fun supportsLongPolling(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && server != null
    }

    /**
     * Request page from server.
     */
    private fun requestPageUpdate() {
        val serverHandler = connectionFactory.createServerHandler(server!!.toGeneric(), context)

        serverHandler.requestPageRx(page)
                .compose(this.bindToLifecycle())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { ohLinkedPage ->
                    log.d(TAG, "Received update " + ohLinkedPage.getWidgets().size + " widgets from  " + page!!.link)
                    updatePage(ohLinkedPage)
                 }, dataLoadError)
    }

    /**
     * Create longpoller listening for updates of page.
     *
     * @return
     */
    private fun createLongPoller(): Disposable {
        val serverId = server!!.id

        val server = serverLoaderFactory.loadServer(realm, serverId)
        val serverHandler = connectionFactory.createServerHandler(server, context)
        return serverHandler.requestPageUpdatesRx(page)
                .compose(this.bindToLifecycle())
                .compose(RxUtil.newToMainSchedulers())
                .subscribe(Consumer { this.updatePage(it) }, dataLoadError)
    }


    /**
     * Update page.
     *
     * Recreate all widgets needed.
     *
     * @param page the page to show.
     * @param force true to invalidate all widgets, false to do if needed.
     */
    @Synchronized private fun updatePage(page: OHLinkedPage?, force: Boolean) {
        if (page == null || page.widgets == null) return

        this.page = page
        val pageWidgets = page.widgets
        val invalidate = !canBeUpdated(widgets, pageWidgets) || force

        if (invalidate) {
            invalidateWidgets(pageWidgets)
        } else {
            updateWidgets(pageWidgets)
        }

        view.updatePage(page)
    }

    /**
     * Update page.
     * Invalidate widgets if possible.
     *
     * @param page
     */
    @Synchronized private fun updatePage(page: OHLinkedPage) {
        updatePage(page, false)
    }

    /**
     * Check if item can be updgraded without replacing widget.
     * @param widget1 first widget to check.
     * @param widget2 second widget to check.
     * @return true if widget can be updated, else false.
     */
    fun canBeUpdated(widget1: OHWidget, widget2: OHWidget): Boolean {
        if (widget1.type != widget2.type) {
            return false
        }

        if (widget1.item == null && widget2.item == null) {
            return true
        }

        return if (widget1.item != null && widget2.item != null) {
            widget1.item.type == widget2.item.type
        } else false

    }

    /**
     * Check if item can be updgraded without replacing widget.
     * @param widgetSet1 first widget set to check.
     * @param widgetSet2 second widget set to check.
     * @return true if widget can be updated, else false.
     */
    fun canBeUpdated(widgetSet1: List<OHWidget>, widgetSet2: List<OHWidget>): Boolean {
        val invalidate = widgetSet1.size != widgetSet2.size
        if (!invalidate) {
            for (i in widgetSet1.indices) {
                val currentWidget = widgetSet1[i]
                val newWidget = widgetSet2[i]

                // TODO check if widget needs updating
                if (!canBeUpdated(currentWidget, newWidget)) {
                    log.d(TAG, "Widget " + currentWidget.type + " " + currentWidget.label + " needs update")
                    return false
                }
            }
            return true
        }
        return false
    }

    /**
     * Invalidate all widgets in page.
     * @param pageWidgets the widgets to update.
     */
    private fun invalidateWidgets(pageWidgets: List<OHWidget>) {
        log.d(TAG, "Invalidate widgets")
        widgetHolders.clear()

        for (widget in pageWidgets) {
            try {
                val widgetView = widgetFactory.createWidget(fragment.context, server!!.toGeneric(), page, widget, null)
                widgetHolders.add(widgetView)
            } catch (e: Exception) {
                log.w(TAG, "Create widget failed", e)
            }

        }
        view.setWidgets(widgetHolders)

        widgets.clear()
        widgets.addAll(pageWidgets)
    }

    /**
     * Update widgets in page.
     * @param pageWidgets the data to update widgets with.
     */
    private fun updateWidgets(pageWidgets: List<OHWidget>) {
        for (i in widgetHolders.indices) {

            try {
                val holder = widgetHolders[i]

                log.d(TAG, "updating widget " + holder.javaClass.simpleName)
                val newWidget = pageWidgets[i]

                holder.update(newWidget)
            } catch (e: Exception) {
                log.w(TAG, "Updating widget failed", e)
            }

        }
    }

    companion object {

        private val TAG = PagePresenter::class.java.simpleName
    }
}