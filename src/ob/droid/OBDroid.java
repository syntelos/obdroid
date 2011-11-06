package ob.droid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * 
 */
public class OBDroid extends Activity
{


    public OBDroid(){
        super();
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(this);
    }
}
