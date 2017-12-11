package treehou.se.habit.ui.servers.create


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.*

import treehou.se.habit.R
import treehou.se.habit.ui.servers.create.myopenhab.CreateMyOpenhabFragment


class CreateServerFragment : Fragment() {

    lateinit var unbinder: Unbinder

    @BindView(R.id.background) lateinit var background: View
    @BindView(R.id.create_server_options) lateinit var createServerOptions: View
    @BindView(R.id.my_openhab_text) lateinit var myOpenhabText: View

    @BindView(R.id.my_openhab_icon) lateinit var myOpenhabIcon: View

    @BindView(R.id.add_my_openhab) lateinit var addMyOpenhabButton: View
    @BindView(R.id.scan_for_servers) lateinit var scanForServersButton: View
    @BindView(R.id.add_new_server) lateinit var newServerButton: View
    @BindViews(R.id.add_my_openhab, R.id.scan_for_servers, R.id.add_new_server) lateinit var buttons: List<@JvmSuppressWildcards View>

    var introFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_server, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        unbinder = ButterKnife.bind(this, view)

        introFinished = true
    }

    @OnClick(R.id.add_my_openhab)
    fun startCreateMyOpenhabServerFlow(){
        val fragmentManager = fragmentManager
        if(fragmentManager != null) {
            val createMyOpenhabFragment = CreateMyOpenhabFragment()
            fragmentManager.beginTransaction()
                    .replace((view!!.parent as ViewGroup).id, createMyOpenhabFragment)
                    .addToBackStack(null)
                    .commit()
        }
    }

    /**
     * Close activity
     */
    fun close() {
        fragmentManager?.popBackStack();
    }

    /**
     * Show item options
     */
    fun showOptions() {
        createServerOptions.visibility = View.VISIBLE
    }

    /**
     * Hide item options
     */
    fun hideOptions() {
        createServerOptions.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        unbinder.unbind()
    }

    companion object {

        fun createFragment(): Fragment {
            val createServerFragment = CreateServerFragment()
            return createServerFragment
        }
    }
}
