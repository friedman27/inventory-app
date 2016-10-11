package com.example.android.inventoryapp;


import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.inventoryapp.data.InventoryContract.ProductEntry;

import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_PRICE;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_QUANTITY;
import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.COLUMN_PRODUCT_SOLD;


public class InventoryCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = InventoryCursorAdapter.class.getSimpleName();

    private TextView mQuantityTextView;
    private TextView mSoldTextView;
    private Context mContext;
    private int mIdColumnIndex;
    private int mRowId;
    private String mQuantityString;
    private String mSoldString;
    private int mRowsAffected;


    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Create and return new blank list item
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Populate list item view with product data
        TextView nameTextView = (TextView) view.findViewById(R.id.product_name);
        mQuantityTextView = (TextView) view.findViewById(R.id.product_quantity);
        mSoldTextView = (TextView) view.findViewById(R.id.product_sold);
        TextView priceTextView = (TextView) view.findViewById(R.id.product_price);
        Button productSaleButton = (Button) view.findViewById(R.id.track_sale);

        // Get the ID for the product row
        mRowId = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));
        // Context variable for the context passed in
        mContext = context;
        // Cursor variable for the cursor passed in from the main activity
        mCursor = cursor;

        String productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME));
        String productQuantity = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_QUANTITY));
        String productPrice = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_PRICE));
        String productSold = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_SOLD));

        nameTextView.setText(productName);
        mQuantityTextView.setText(productQuantity);
        priceTextView.setText(productPrice);
        mSoldTextView.setText(productSold);

        productSaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(LOG_TAG, "sale button was pressed");

                int rowsAffected = productSale();

                if (rowsAffected != 0) {
                    // update text view if db update is successful
                    mQuantityTextView.setText(mQuantityString);
                    mSoldTextView.setText(mSoldString);
                }
            }
        });
    }

    public int productSale() {
        int soldQuantity = Integer.parseInt(mSoldTextView.getText().toString());
        int quantity = Integer.parseInt(mQuantityTextView.getText().toString());

        if (quantity > 0) {
            quantity--;
            soldQuantity++;

            mQuantityString = Integer.toString(quantity);
            mSoldString = Integer.toString(soldQuantity);

            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, mQuantityString);
            values.put(ProductEntry.COLUMN_PRODUCT_SOLD, mSoldString);

            Log.e(LOG_TAG, "Value of mRowID: " + mRowId);
            Log.e(LOG_TAG, "Value of mColumnIndex " + mIdColumnIndex);

            Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.PRODUCT_TABLE_CONTENT_URI,
                    mRowId);

            Log.e(LOG_TAG, "currentProductUri: " + String.valueOf(currentProductUri));

            mRowsAffected = mContext.getContentResolver().update(currentProductUri, values,
                    null, null);
        }
        return mRowsAffected;
    }
}
