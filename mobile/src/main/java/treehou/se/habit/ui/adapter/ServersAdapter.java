package treehou.se.habit.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import treehou.se.habit.R;
import treehou.se.habit.core.db.model.ServerDB;

public class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ServerHolder> {

    private RealmResults<ServerDB> realmResults;
    private ItemListener itemListener = new DummyItemListener();

    public class ServerHolder extends RecyclerView.ViewHolder {
        public final TextView lblName;

        public ServerHolder(View view) {
            super(view);
            lblName = (TextView) view.findViewById(R.id.lbl_server);
        }
    }

    public ServersAdapter(final RealmResults<ServerDB> realmResults) {
        this.realmResults = realmResults;
    }

    @Override
    public ServerHolder onCreateViewHolder(ViewGroup parent, int position) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.item_server, parent, false);

        return new ServerHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ServerHolder serverHolder, final int position) {
        ServerDB server = realmResults.get(position);

        serverHolder.lblName.setText(server.getDisplayName());
        serverHolder.itemView.setOnClickListener(v -> itemListener.onItemClickListener(serverHolder));
        serverHolder.itemView.setOnLongClickListener(v -> itemListener.onItemLongClickListener(serverHolder));
    }

    @Override
    public int getItemCount() {
        return realmResults.size();
    }

    public ServerDB getItem(int position) {
        return realmResults.get(position);
    }

    public interface ItemListener {

        void onItemClickListener(ServerHolder serverHolder);

        boolean onItemLongClickListener(ServerHolder serverHolder);
    }

    public class DummyItemListener implements ItemListener {

        @Override
        public void onItemClickListener(ServerHolder serverHolder) {
        }

        @Override
        public boolean onItemLongClickListener(ServerHolder serverHolder) {
            return false;
        }
    }

    /**
     * Add adapter change listener
     * @param itemListener
     */
    public void setItemListener(ItemListener itemListener) {
        if (itemListener == null) {
            this.itemListener = new DummyItemListener();
            return;
        }
        this.itemListener = itemListener;
    }
}
