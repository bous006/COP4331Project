package group15.cop4331project;

import android.content.ContentValues;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import group15.cop4331project.data.ReportContract.ReportEntry;


/**
 * This activity hold the "My Reports" and "Recent Reports" fragments
 * Using fragments allows us to swipe between these pages making the UI more user friendly
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the view pager that will allow the user to swipe between fragments
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        //Create an Adapter that knows which fragment should be on which page
        PageAdapter adapter = new PageAdapter(this, getSupportFragmentManager());

        //Set the adapter onto the view pager
        viewPager.setAdapter(adapter);

        //Find the tab layout that shows the tabs
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        // Connect the tab layout with the view pager. This will
        tabLayout.setupWithViewPager(viewPager);
    }
    /**
     * This is really just for us so that we can clear the db easily
     */
    private void deleteAllReports() {
        int rowsDeleted = getContentResolver().delete(ReportEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from pet database");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertReport();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllReports();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void insertReport() {
        // Create a ContentValues object where column names are the keys,
        // and Toto's pet attributes are the values.
        Calendar calendar = Calendar.getInstance();
        Long date = calendar.getTimeInMillis();
        
        ContentValues values = new ContentValues();
        values.put(ReportEntry.COLUMN_REPORT_NAME, "Kid Pooped on Table");
        values.put(ReportEntry.COLUMN_REPORT_TYPE, ReportEntry.TYPE_ASSAULT);
        values.put(ReportEntry.COLUMN_REPORT_DATE, date);
        values.put(ReportEntry.COLUMN_REPORT_DESCRIPTION, "There was this kid and he pooped on a table.");

        // Insert a new row for Toto into the provider using the ContentResolver.
        // Use the {@link PetEntry#CONTENT_URI} to indicate that we want to insert
        // into the pets database table.
        // Receive the new content URI that will allow us to access Toto's data in the future.
        Uri newUri = getContentResolver().insert(ReportEntry.CONTENT_URI, values);
    }
}
