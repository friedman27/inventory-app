package com.example.android.inventoryapp;


import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventoryapp.data.InventoryContract.ProductEntry;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int PRODUCT_LOADER = 0;

    private InventoryCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.product_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
            }
        });

        ListView productListView = (ListView) findViewById(R.id.list);
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        mAdapter = new InventoryCursorAdapter(this, null);
        productListView.setAdapter(mAdapter);

        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // use getIntent() and getData() to get the associated URI
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);

                // Create a new URI with the ID of whatever product is being updated
                Uri currentProductURI = ContentUris.withAppendedId(ProductEntry.PRODUCT_TABLE_CONTENT_URI, id);

                // Sets the data from the intent onto the new URI
                intent.setData(currentProductURI);

                // Launch the detail activity with the new information
                startActivity(intent);
            }
        });
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // takes action based on the loader id passed in from the initLoader in onCreate
        switch (id) {
            case PRODUCT_LOADER:
                String[] projection = {
                        ProductEntry._ID,
                        ProductEntry.COLUMN_PRODUCT_NAME,
                        ProductEntry.COLUMN_PRODUCT_QUANTITY,
                        ProductEntry.COLUMN_PRODUCT_SOLD,
                        ProductEntry.COLUMN_PRODUCT_PRICE,
                        ProductEntry.COLUMN_SUPPLIER_EMAIL
                };

                return new CursorLoader(this, ProductEntry.PRODUCT_TABLE_CONTENT_URI, projection, null, null, null);

            default:
                // if an invalid ID was passed
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_mainl file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteAllProducts();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Helper method to delete all products in the database.
     */
    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(ProductEntry.PRODUCT_TABLE_CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from database");
    }
}
