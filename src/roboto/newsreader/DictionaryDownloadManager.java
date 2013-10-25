package roboto.newsreader;

import android.app.DownloadManager;
import android.content.Context;

/**
 * Created with IntelliJ IDEA.
 * User: nareshgowdidiga
 * Date: 20/10/13
 * Time: 9:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class DictionaryDownloadManager {

    DownloadManager androidDownloadManager;

    public DictionaryDownloadManager(Context context){
        androidDownloadManager =  (DownloadManager)context.getSystemService(context.DOWNLOAD_SERVICE);
    }

    public void downloadWordNetEnglishDictionary(){
       // androidDownloadManager.enqueue(request);
    };
    public void downloadWordNetSpanishDictionary(){};
}
