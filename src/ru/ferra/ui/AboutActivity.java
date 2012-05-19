package ru.ferra.ui;


import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import ru.ferra.R;

public class AboutActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.about);
        TextView aboutText = (TextView)findViewById(R.id.about_version);

        try {
            String version = getString(R.string.version) + " " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            aboutText.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
