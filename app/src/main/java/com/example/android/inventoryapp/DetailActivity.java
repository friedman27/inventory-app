package com.example.android.inventoryapp;

/*
The detail layout for each item displays the remainder of the information stored in the database.

The detail layout contains buttons to modify the current quantity either by tracking a sale or by receiving a shipment.

The detail layout contains a button to order from the supplier.

The detail view contains a button to delete the product record entirely.
 */

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.ProductEntry;

import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_PRICE;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_QUANTITY;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_SOLD;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_SUPPLIER_EMAIL;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry._ID;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>  {

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    /** Identifier for the pet data loader */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    /** EditText field to enter the product name */
    private EditText mNameEditText;

    /** EditText field to enter the product quantity */
    private EditText mQuantityEditText;

    /** EditText field to enter the product quantity */
    private EditText mSoldEditText;

    /** EditText field to enter the product price */
    private EditText mPriceEditText;

    /** EditText field to enter the supplier's email address */
    private EditText mEmailAddressEditText;

    /** Button to order more of the current product from the supplier */
    private Button mOrderMoreButton;

    // Uri of the current product
    private Uri mCurrentProductUri;

    // Will be true of the user edits part of the form
    private boolean mProductHasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();
        Log.e(LOG_TAG, "Value of current uri: " + mCurrentProductUri);

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.add_product));
        } else {
            setTitle(getString(R.string.edit_product));
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mSoldEditText = (EditText) findViewById(R.id.edit_sold);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mEmailAddressEditText = (EditText) findViewById(R.id.supplier_email);
        mOrderMoreButton = (Button) findViewById(R.id.order_more);

        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mEmailAddressEditText.setOnTouchListener(mTouchListener);

        if (mCurrentProductUri != null) {
            // Click button for "Order More", get sent to an email intent to request more from the supplier
            mOrderMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    orderMore();
                }
            });
        }

        // Click button for "Item Sold" on detail layout, update quantity and sold numbers
        Button itemSoldButton = (Button) findViewById(R.id.item_sold);
        itemSoldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemSold();
            }
        });

        // Click button for "Shipment Received" on detail layout, update quantity and sold numbers
        Button shipmentReceivedButton = (Button) findViewById(R.id.shipment_received);
        shipmentReceivedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shipmentReceived();
            }
        });
    }

    /*
        Executed when the "Order More" button is clicked, creating an order statement to be opened
        in an email using composeEmail() method below.
    */
    public void orderMore() {
        Log.e(LOG_TAG, "order more button was pressed.");
        String[] projection = {
                _ID,
                COLUMN_PRODUCT_NAME,
                COLUMN_PRODUCT_QUANTITY,
                COLUMN_PRODUCT_SOLD,
                COLUMN_PRODUCT_PRICE,
                COLUMN_SUPPLIER_EMAIL};

        Cursor cursor = getContentResolver().query(mCurrentProductUri, projection, null, null, null);

        if (cursor.moveToFirst()) {
            String productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME));
            String productID = cursor.getString(cursor.getColumnIndexOrThrow(_ID));
            String emailAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPLIER_EMAIL));

            composeEmail(emailAddress, getString(R.string.email_subject),
                    getString(R.string.email_message_id) + " " + productID +
                            getString(R.string.email_message_name) + " " + productName +
                            getString(R.string.email_message_quantity) + " " +
                            getString(R.string.email_message_thanks));
        }
    }

    // Helper method to compose an email in a new intent with an address, subject, and message
    public void composeEmail(String address, String subject, String message) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_EMAIL, address);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    // Executed when "Item Sold" button is pressed, adds to sold and takes away from quantity
    public void itemSold() {
        Log.e(LOG_TAG, "Item Sold button was pressed");
        int soldQuantity = Integer.parseInt(mSoldEditText.getText().toString());
        int quantity = Integer.parseInt(mQuantityEditText.getText().toString());

        if (quantity > 0) {
            quantity--;
            soldQuantity++;

            String quantityString = Integer.toString(quantity);
            String soldString = Integer.toString(soldQuantity);

            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);
            values.put(ProductEntry.COLUMN_PRODUCT_SOLD, soldString);

            int rowsAffected = getContentResolver().update(mCurrentProductUri, values,
                    null, null);

            if (rowsAffected != 0) {
                // update text view if db update is successful
                mQuantityEditText.setText(quantityString);
                mSoldEditText.setText(soldString);
            }
        }
    }

    // Executed when "Item Sold" button is pressed, adds to sold and takes away from quantity
    public void shipmentReceived() {
        Log.e(LOG_TAG, "Shipment Received button was pressed");
        int soldQuantity = Integer.parseInt(mSoldEditText.getText().toString());
        int quantity = Integer.parseInt(mQuantityEditText.getText().toString());

        quantity++;

        String quantityString = Integer.toString(quantity);

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);

        int rowsAffected = getContentResolver().update(mCurrentProductUri, values,
                null, null);

        if (rowsAffected != 0) {
            // update text view if db update is successful
            mQuantityEditText.setText(quantityString);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                COLUMN_PRODUCT_NAME,
                COLUMN_PRODUCT_QUANTITY,
                COLUMN_PRODUCT_SOLD,
                COLUMN_PRODUCT_PRICE,
                COLUMN_SUPPLIER_EMAIL
        };
        CursorLoader cursorLoader = new CursorLoader(this, mCurrentProductUri, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_QUANTITY);
            int soldColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_SOLD);
            int priceColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_PRICE);
            int emailColumnIndex = cursor.getColumnIndex(COLUMN_SUPPLIER_EMAIL);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int sold = cursor.getInt(soldColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            String email = cursor.getString(emailColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mQuantityEditText.setText(Integer.toString(quantity));
            mSoldEditText.setText(Integer.toString(sold));
            mPriceEditText.setText(Double.toString(price));
            mEmailAddressEditText.setText(email);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mSoldEditText.setText("");
        mPriceEditText.setText("");
        mEmailAddressEditText.setText("");
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    /**
     * Get user input from editor and save new product into database.
     */
    private void saveProduct() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String soldString = mSoldEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String emailString = mEmailAddressEditText.getText().toString().trim();



        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(quantityString) &&
                TextUtils.isEmpty(priceString) && TextUtils.isEmpty(emailString) && TextUtils.isEmpty(soldString)) {
            Toast.makeText(this, R.string.no_changes_made, Toast.LENGTH_SHORT).show();
            // Since no fields were modified, we can return early without creating a new pet.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and attributes from the editor are the values.
        ContentValues values = new ContentValues();

        // add name to values
        values.put(COLUMN_PRODUCT_NAME, nameString);

        // add quantity to values
        int quantity;
        if (!TextUtils.isEmpty(quantityString)) {
            try {
                quantity = Integer.parseInt(quantityString);
                values.put(COLUMN_PRODUCT_QUANTITY, quantity);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Sold quantity number was invalid and did not update",
                        Toast.LENGTH_SHORT).show();
            }
        }


        int sold;
        int defaultSold = 0;
        if(soldString.isEmpty()) {
            sold = defaultSold;
        } else {
            try{
                sold = Integer.parseInt(soldString);
            }catch (NumberFormatException e) {
                sold = defaultSold;
                Toast.makeText(this, "Sold quantity number was invalid and did not update",
                        Toast.LENGTH_SHORT).show();
            }
        }
        soldString = String.valueOf(sold);
        values.put(COLUMN_PRODUCT_SOLD, soldString);

        // add price to values
        double price = 0.00;
        if (!TextUtils.isEmpty(quantityString)) {
            price = Double.parseDouble(priceString);
            Log.e(LOG_TAG, "price value in saveProduct method: " + price);
        }
        values.put(COLUMN_PRODUCT_PRICE, price);
        values.put(COLUMN_SUPPLIER_EMAIL, emailString);

        // If/else clause to ensure no fields are left empty before the information is sent to the provider
        if (!TextUtils.isEmpty(nameString) && !TextUtils.isEmpty(quantityString) &&
                !TextUtils.isEmpty(priceString) && !TextUtils.isEmpty(emailString) && !TextUtils.isEmpty(soldString)) {

            // Determines whether the product exists already and then inserts a new product if it does not
            if (mCurrentProductUri == null) {
                // Insert a new row for product in the database, returning the ID of that new row.
                Uri newUri = getContentResolver().insert(ProductEntry.PRODUCT_TABLE_CONTENT_URI, values);

                if (newUri == null) {
                    Toast.makeText(this, R.string.editor_insert_product_failed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.editor_insert_product_succeeded, Toast.LENGTH_SHORT).show();
                }
            // If a product already exists with the specified uri, then it updates the product instead
            } else {
                int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(this, getString(R.string.editor_update_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_update_product_succeeded),
                            Toast.LENGTH_SHORT).show();
                }
            }
        // Displays toast messages to user if they did not complete one of the required fields (all fields)
        } else {
            if (TextUtils.isEmpty(nameString)) {
                Toast.makeText(this, "Need to add a value for name",
                        Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(quantityString)) {
                Toast.makeText(this, "Need to add a value for quantity",
                        Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(priceString)) {
                Toast.makeText(this, "Need to add a value for price",
                        Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(soldString)) {
                Toast.makeText(this, "Need to add a value for amount sold",
                        Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(emailString)) {
                Toast.makeText(this, "Need to add a value for supplier email",
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.e(LOG_TAG, getString(R.string.editor_insert_product_failed));
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_detail.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                saveProduct();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
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

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        int rowsDeleted = 0;
        if (mCurrentProductUri != null) {
            rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
        }
        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0) {
            // If no rows were deleted, then there was an error with the delete.
            Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                    Toast.LENGTH_SHORT).show();
        }
        // Close the activity
        finish();
    }
}
