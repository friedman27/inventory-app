package com.example.android.inventoryapp.data;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.inventoryapp.data.InventoryContract.ProductEntry;

public class InventoryProvider extends ContentProvider {

    private static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    // Database Helper Object
    private InventoryDbHelper mDbHelper;

    /** URI matcher code for the content URI for the products table */
    private static final int PRODUCT_MATCHER_TABLE_CODE = 100;

    /** URI matcher code for the content URI for a single products in the products table */
    private static final int PRODUCT_MATCHER_ITEM_CODE = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS, PRODUCT_MATCHER_TABLE_CODE);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_MATCHER_ITEM_CODE);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCT_MATCHER_TABLE_CODE:
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCT_MATCHER_ITEM_CODE:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCT_MATCHER_TABLE_CODE:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_MATCHER_ITEM_CODE:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCT_MATCHER_TABLE_CODE:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertProduct(Uri uri, ContentValues values) {
        String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (name.isEmpty() || name == null) {
            throw new IllegalArgumentException("Product requires a name.");
        }

        Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Product requires a valid quantity.");
        }

        Integer sold = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_SOLD);
        if (sold == null || sold < 0) {
            throw new IllegalArgumentException("Product requires a valid amount sold.");
        }

        Double price = values.getAsDouble(ProductEntry.COLUMN_PRODUCT_PRICE);
        Log.e(LOG_TAG, "Value of price in insert(): " + price);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Product requires a valid price.");
        }

        String email = values.getAsString(ProductEntry.COLUMN_SUPPLIER_EMAIL);
        if (email == null) {
            throw new IllegalArgumentException("Product requires a valid email.");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(ProductEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        // Notifies all listeners that the data has changed for the URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the uri of the newly inserted row
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCT_MATCHER_TABLE_CODE:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_MATCHER_ITEM_CODE:
                // Delete a single row given by the ID in the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PRODUCT_MATCHER_TABLE_CODE:
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCT_MATCHER_ITEM_CODE:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateProduct(Uri uri, ContentValues values, String selection, String [] selectionArgs) {

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Product requires a name.");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY)) {
            Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Product requires a valid quantity.");
            }
        }


        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            Float price = values.getAsFloat(ProductEntry.COLUMN_PRODUCT_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException("Product requires a valid price.");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_SUPPLIER_EMAIL)) {
            String email = values.getAsString(ProductEntry.COLUMN_SUPPLIER_EMAIL);
            if (email == null) {
                email = "";
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }
}
