package roboto.newsreader;


import android.app.Activity;
import android.app.DownloadManager;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.roboto.R;

import java.io.FileNotFoundException;


/**
 * Created with IntelliJ IDEA.
 * User: nareshgowdidiga
 * Date: 20/10/13
 * Time: 9:52 PM
 * To change this template use File | Settings | File Templates.
 */

public class AndroidDownloadActivity extends Activity {
    String Download_path = "http://goo.gl/Mfyya";
    String Download_ID = "DOWNLOAD_ID";
    SharedPreferences preferenceManager;
    DownloadManager downloadManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        preferenceManager = PreferenceManager.getDefaultSharedPreferences(this);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        Button btnDownload = (Button) findViewById(R.id.download);
        btnDownload.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Uri Download_Uri = Uri.parse(Download_path);
                DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
                request.setDestinationInExternalFilesDir(getApplicationContext(), null, "roboto");
                long download_id = downloadManager.enqueue(request);

                //Save the download id
                Editor PrefEdit = preferenceManager.edit();
                PrefEdit.putLong(Download_ID, download_id);
                PrefEdit.commit();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(downloadReceiver);
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(preferenceManager.getLong(Download_ID, 0));
            Cursor cursor = downloadManager.query(query);

            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);
                int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int reason = cursor.getInt(columnReason);

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    //Retrieve the saved download id
                    long downloadID = preferenceManager.getLong(Download_ID, 0);

                    ParcelFileDescriptor file;
                    try {
                        file = downloadManager.openDownloadedFile(downloadID);
                        Toast.makeText(AndroidDownloadActivity.this,
                                "File Downloaded: " + file.toString(),
                                Toast.LENGTH_LONG).show();
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Toast.makeText(AndroidDownloadActivity.this,
                                e.toString(),
                                Toast.LENGTH_LONG).show();
                    }

                } else if (status == DownloadManager.STATUS_FAILED) {
                    Toast.makeText(AndroidDownloadActivity.this,
                            "FAILED!\n" + "reason of " + reason,
                            Toast.LENGTH_LONG).show();
                } else if (status == DownloadManager.STATUS_PAUSED) {
                    Toast.makeText(AndroidDownloadActivity.this,
                            "PAUSED!\n" + "reason of " + reason,
                            Toast.LENGTH_LONG).show();
                } else if (status == DownloadManager.STATUS_PENDING) {
                    Toast.makeText(AndroidDownloadActivity.this,
                            "PENDING!",
                            Toast.LENGTH_LONG).show();
                } else if (status == DownloadManager.STATUS_RUNNING) {
                    Toast.makeText(AndroidDownloadActivity.this,
                            "RUNNING!",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

    };

}
