package com.example.android.inventoryapp.data;


import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class InventoryContract {

    // String for content authority, matches the manifest authority
    public static final String CONTENT_AUTHORITY = "com.example.android.inventoryapp";

    // String for the URI to be used for any content in this app, will be used with the table path
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // String for the pets table to be used at the end of BASE_CONTENT_URI
    public static final String PATH_PRODUCTS = "products";

    private InventoryContract() {}

    public static final class ProductEntry implements BaseColumns {

        /**
         * The MIME type of the {@link #PRODUCT_TABLE_CONTENT_URI} for a list of pets.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * The MIME type of the {@link #PRODUCT_TABLE_CONTENT_URI} for a single pet.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        // URI String for this table
        public static final Uri PRODUCT_TABLE_CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        public static final String TABLE_NAME = "products";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_PRODUCT_NAME = "name";
        public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
        public static final String COLUMN_PRODUCT_PRICE = "price";
        public static final String COLUMN_SUPPLIER_EMAIL = "email";
        public static final String COLUMN_PRODUCT_SOLD = "sold";
    }
}
