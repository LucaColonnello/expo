package expo.modules.updates;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import expo.modules.updates.db.Reaper;
import expo.modules.updates.db.UpdatesDatabase;
import expo.modules.updates.db.entity.AssetEntity;
import expo.modules.updates.db.entity.UpdateEntity;
import expo.modules.updates.loader.EmbeddedLoader;
import expo.modules.updates.loader.RemoteLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdatesController {

  private static final String TAG = UpdatesController.class.getSimpleName();

  private static String UPDATES_DIRECTORY_NAME = ".expo";
  private static String URL_PLACEHOLDER = "EXPO_APP_URL";

  private static UpdatesController sInstance;

  private Context mContext;
  private Uri mManifestUrl;
  private File mUpdatesDirectory;
  private UpdatesDatabase mDatabase;
  private RemoteLoader mRemoteLoader;

  private UpdateEntity mLaunchedUpdate;

  public static UpdatesController getInstance() {
    return sInstance;
  }

  public static void initialize(Context context) {
    if (sInstance == null) {
      String urlString = context.getString(R.string.expo_app_url);
      Uri url = URL_PLACEHOLDER.equals(urlString) ? null : Uri.parse(urlString);
      new UpdatesController(context, url);
    }
  }

  private UpdatesController(Context context, Uri url) {
    sInstance = this;
    mContext = context;
    mManifestUrl = url;
    mUpdatesDirectory = getOrCreateUpdatesDirectory();
    mDatabase = UpdatesDatabase.getInstance(context);
  }

  public void start() {
    new EmbeddedLoader(mContext, mDatabase, mUpdatesDirectory).loadEmbeddedUpdate();
    mLaunchedUpdate = launchUpdate();
    if (mRemoteLoader == null && mManifestUrl != null) {
      mRemoteLoader = new RemoteLoader(mContext, mDatabase, mUpdatesDirectory);
      mRemoteLoader.start(mManifestUrl, new RemoteLoader.LoaderCallback() {
        @Override
        public void onFailure(Exception e) {
          Log.e("erictest", "failure", e);
        }

        @Override
        public void onSuccess(UpdateEntity update) {
          Log.d("erictest", "success");
          Reaper.reapUnusedUpdates(mDatabase, mUpdatesDirectory, mLaunchedUpdate);
        }
      });
    }
  }

  private File getOrCreateUpdatesDirectory() {
    File updatesDirectory = new File(mContext.getFilesDir(), UPDATES_DIRECTORY_NAME);
    boolean exists = updatesDirectory.exists();
    boolean isFile = updatesDirectory.isFile();
    if (!exists || isFile) {
      if (isFile) {
        if (!updatesDirectory.delete()) {
          // TODO: throw error
        }
      }

      if (!updatesDirectory.mkdir()) {
        // TODO: throw error
      }
    }
    return updatesDirectory;
  }

  private UpdateEntity launchUpdate() {
    List<UpdateEntity> launchableUpdates = mDatabase.updateDao().loadLaunchableUpdates();

    String versionName = UpdateUtils.getBinaryVersion(mContext);

    if (versionName != null) {
      List<UpdateEntity> launchableUpdatesCopy = new ArrayList<>(launchableUpdates);
      for (UpdateEntity update : launchableUpdatesCopy) {
        String[] binaryVersions = update.binaryVersions.split(",");
        boolean matches = false;
        for (String version : binaryVersions) {
          if (version.equals(versionName)) {
            matches = true;
            break;
          }
        }
        if (!matches) {
          launchableUpdates.remove(update);
        }
      }
    }

    return new SelectionPolicyNewest().selectUpdateToLaunch(launchableUpdates);
  }

  public String getLaunchAssetFile() {
    if (mLaunchedUpdate == null) {
      return null;
    }

    String relativePath = mDatabase.updateDao().loadLaunchAssetUrl(mLaunchedUpdate.id);
    return relativePath != null ? new File(mUpdatesDirectory, relativePath).toString() : null;
  }

  public Map<String, String> getLocalAssetFiles() {
    if (mLaunchedUpdate == null) {
      return null;
    }

    List<AssetEntity> assetEntities = mDatabase.assetDao().loadAssetsForUpdate(mLaunchedUpdate.id);
    if (assetEntities == null) {
      return null;
    }
    Map<String, String> localAssetFiles = new HashMap<>();
    for (int i = 0; i < assetEntities.size(); i++) {
      String filename = assetEntities.get(i).relativePath;
      if (filename != null) {
        localAssetFiles.put(
            assetEntities.get(i).url.toString(),
            new File(mUpdatesDirectory, filename).toString()
        );
      }
    }
    return localAssetFiles;
  }
}
