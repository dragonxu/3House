package treehou.se.habit.tasker.reciever;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import javax.inject.Inject;

import io.realm.Realm;
import se.treehou.ng.ohcommunicator.services.Connector;
import se.treehou.ng.ohcommunicator.services.IServerHandler;
import treehou.se.habit.core.db.model.ItemDB;
import treehou.se.habit.tasker.boundle.CommandBoundleManager;
import treehou.se.habit.util.ConnectionFactory;
import treehou.se.habit.util.Util;

public class CommandReciever implements IFireReciever {

    public static final String TAG = "CommandReciever";

    public static final int TYPE = CommandBoundleManager.TYPE_COMMAND;

    public static final String BUNDLE_EXTRA_COMMAND = "treehou.se.habit.extra.COMMAND";
    public static final String BUNDLE_EXTRA_ITEM    = "treehou.se.habit.extra.ITEM";

    @Inject ConnectionFactory connectionFactory;

    public boolean isBundleValid(Bundle bundle) {
        if (null == bundle) {
            Log.e(TAG, "Bundle cant be null");
            return false;
        }

        if (!bundle.containsKey(BUNDLE_EXTRA_COMMAND)) {
            Log.e(TAG, String.format("bundle must contain extra %s", BUNDLE_EXTRA_COMMAND));
            return false;
        }

        if (3 != bundle.keySet().size()) {
            Log.e(TAG, String.format("bundle must contain 3 keys, but currently contains %d keys: %s", bundle.keySet().size(), bundle.keySet()));
            return false;
        }

        if (TextUtils.isEmpty(bundle.getString(BUNDLE_EXTRA_COMMAND))) {
            Log.e(TAG, String.format("bundle extra %s appears to be null or empty.  It must be a non-empty string", BUNDLE_EXTRA_COMMAND)); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    @Override
    public boolean fire(Context context, Bundle bundle) {
        Util.getApplicationComponent(context).inject(this);

        if (isBundleValid(bundle)) {
            final long itemId = bundle.getLong(BUNDLE_EXTRA_ITEM);
            final String command = bundle.getString(BUNDLE_EXTRA_COMMAND);

            Realm realm = Realm.getDefaultInstance();
            ItemDB item = ItemDB.load(realm, itemId);
            if(item != null){
                IServerHandler serverHandler = connectionFactory.createServerHandler(item.getServer().toGeneric(), context);
                serverHandler.sendCommand(item.getName(), command);
                Log.d(TAG, "Sent sendCommand " + command + " to item " + item.getName());
            }else {
                Log.d(TAG, "Item no longer exists");
            }
            realm.close();

            Log.d(TAG, "Sending sendCommand " + command);
        }else {
            Log.d(TAG, "Boundle not valid.");
        }

        return false;
    }
}
