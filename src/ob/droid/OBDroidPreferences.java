package ob.droid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Load preferences from the XML resource
 */
public class OBDroidPreferences
    extends PreferenceActivity
{

    public OBDroidPreferences(){
        super();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

}
