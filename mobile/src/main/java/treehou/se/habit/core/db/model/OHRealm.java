package treehou.se.habit.core.db.model;

import android.content.Context;

import javax.inject.Inject;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmSchema;
import io.realm.annotations.PrimaryKey;

public class OHRealm {

    private Context context;

    public OHRealm(Context context) {
        this.context = context;
    }

    public void setup(Context context) {
        Realm.init(context);
        Realm.setDefaultConfiguration(configuration());
    }

    public RealmConfiguration configuration() {
        return new RealmConfiguration.Builder()
                .modules(new OHRealmModule())
                .migration(migration)
                .name("treehou.realm")
                .schemaVersion(2)
                .build();
    }

    public Realm realm(){
        return Realm.getDefaultInstance();
    }

    RealmMigration migration = new RealmMigration() {
        @Override
        public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

            RealmSchema schema = realm.getSchema();
            switch ((int) oldVersion) {
                case 1:
                    schema.create("SitemapSettingsDB")
                            .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
                            .addField("display", boolean.class);

                    schema.get("SitemapDB")
                            .addRealmObjectField("settingsDB", schema.get("SitemapSettingsDB"));
            }
        }
    };
}
