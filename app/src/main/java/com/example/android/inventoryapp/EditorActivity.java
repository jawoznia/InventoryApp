package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ItemContract.ItemEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Integer.parseInt;

/**
 * Created by Jam on 25.07.2017.
 */

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {


    public static final String LOG_TAG = EditorActivity.class.getSimpleName();
    /**
     * EditText field to enter the items's name
     */
    private EditText mNameEditText;

    private Button mIncreaseBtn;
    private Button mDecreaseBtn;
    /**
     * EditText field to enter the item's quantity
     */
    private EditText mQuantityEditText;

    private Button mOrderButton;
    /**
     * EditText field to enter the item's price
     */
    private EditText mPriceEditText;

    private EditText mSupplierEditText;

    private EditText mSupplierEmailEditText;

    private ImageView mAddImage;

    private Uri mCurrentItemUri;

    private static final int EXISTING_ITEM_LOADER = 0;

    private static final int PICTURE_GALLERY_REQUEST = 5;

    private String picturePath;

    private Bitmap picture;

    final Context mContext = this;

    /**
     * Identifier for the record album image URI loader
     */
    private static final String STATE_PICTURE_URI = "STATE_PICTURE_URI";


    private boolean mItemHasChanged = false;

    private Uri pictureUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();
        if (mCurrentItemUri == null) {
            setTitle(getString(R.string.editor_activity_title_add_item));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_item));
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }
        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mAddImage = (ImageView) findViewById(R.id.edit_item_image_upload_iv);
        mSupplierEditText = (EditText) findViewById(R.id.edit_supplier);
        mSupplierEmailEditText = (EditText) findViewById(R.id.edit_supplier_email);
        mOrderButton = (Button) findViewById(R.id.edit_order_btn);
        mIncreaseBtn = (Button) findViewById(R.id.editor_increase_button);
        mDecreaseBtn = (Button) findViewById(R.id.editor_decrease_btn);

        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mAddImage.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);
        mOrderButton.setOnTouchListener(mTouchListener);
        mDecreaseBtn.setOnTouchListener(mTouchListener);
        mIncreaseBtn.setOnTouchListener(mTouchListener);

        mAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openPictureGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                String pictureDirectoryPath = pictureDirectory.getPath();

                Uri data = Uri.parse(pictureDirectoryPath);

                openPictureGallery.setDataAndType(data, "image/*");

                startActivityForResult(openPictureGallery, PICTURE_GALLERY_REQUEST);
            }
        });
        // Set a clickListener on email button
        mOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String orderQty = mQuantityEditText.getText().toString().trim();
                if (orderQty.length() != 0) {
                    String productName = mNameEditText.getText().toString().trim();

                    String emailAddress = "mailto:" + mSupplierEmailEditText.getText().toString().trim();
                    String subjectHeader = "Order For: " + productName;
                    String orderMessage = "Please send " + orderQty + " units of " + productName + ". " + " \n\n" + "Thank you.";

                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse(emailAddress));
                    intent.putExtra(Intent.EXTRA_SUBJECT, subjectHeader);
                    intent.putExtra(Intent.EXTRA_TEXT, orderMessage);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }

                } else {
                    String toastMessage = "Order quantity required";
                    Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
                }

            }
        });
        mDecreaseBtn.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                String currentProduct = mNameEditText.getText().toString();
                String toastMessage = null;
                int currentQuantity = parseInt(mQuantityEditText.getText().toString());

                if (currentQuantity >= 1) {
                    currentQuantity--;
                    mQuantityEditText.setText(String.valueOf(currentQuantity));
                } else {
                    toastMessage = "You can't reduce your stock of " + currentProduct + " to 0!";
                    mQuantityEditText.setText(String.valueOf(currentQuantity));
                    Toast.makeText(v.getContext(), toastMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
        mIncreaseBtn.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                int currentQuantity = parseInt(mQuantityEditText.getText().toString());

                currentQuantity++;
                mQuantityEditText.setText(String.valueOf(currentQuantity));
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // checking if the request code and result code match our request
        if (requestCode == PICTURE_GALLERY_REQUEST && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                try {
                    //this is the address of the image on the sd cards
                    pictureUri = resultData.getData();
                    int takeFlags = resultData.getFlags();
                    takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    picturePath = pictureUri.toString();
                    //Declare a stream to read the data from the card
                    InputStream inputStream;
                    //We are getting an input stream based on the Uri of the image
                    inputStream = getContentResolver().openInputStream(pictureUri);
                    //Get a bitmap from the stream
                    picture = BitmapFactory.decodeStream(inputStream);
                    //Show the image to the user
                    mAddImage.setImageBitmap(picture);
                    picturePath = pictureUri.toString();
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            getContentResolver().takePersistableUriPermission(pictureUri, takeFlags);
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    mAddImage.setImageBitmap(getBitmapFromUri(pictureUri, mAddImage));

                } catch (Exception e) {
                    e.printStackTrace();
                    //Show the user a Toast mewssage that the Image is not available
                    Toast.makeText(EditorActivity.this, "Unable to open image", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri, ImageView imageView) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            Bitmap.createScaledBitmap(bitmap, 88, 88, false);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
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
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        int rowsDeleted = 0;
        if (mCurrentItemUri != null) {
            rowsDeleted = getContentResolver().delete(
                    mCurrentItemUri,
                    null,
                    null
            );
        }
        if (rowsDeleted == 0)
            Toast.makeText(this, R.string.no_item_deleted, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, R.string.item_deleted, Toast.LENGTH_SHORT).show();
        finish();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                // user clicked the "Keep editing" button, so dismiss the dialog
                // and continue
                if (dialog != null)
                    dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (pictureUri != null)
            outState.putString(STATE_PICTURE_URI, pictureUri.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_PICTURE_URI) &&
                !savedInstanceState.getString(STATE_PICTURE_URI).equals("")) {
            pictureUri = Uri.parse(savedInstanceState.getString(STATE_PICTURE_URI));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    private void saveItem() {
        if (pictureUri != null) {
            String nameString = mNameEditText.getText().toString().trim();
            String quantityString = mQuantityEditText.getText().toString().trim();
            String priceString = mPriceEditText.getText().toString().trim();
            String supplierString = mSupplierEditText.getText().toString().trim();
            String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();
            if (nameString.isEmpty() && quantityString.isEmpty() && priceString.isEmpty())
                return;
            if (quantityString.isEmpty())
                quantityString = "0";
            int quantity = parseInt(quantityString);
            // Create a String that contains the SQL statement to create the items table

            picturePath = pictureUri.toString().trim();

            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
            values.put(ItemEntry.COLUMN_ITEM_PRICE, priceString);
            values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
            values.put(ItemEntry.COLUMN_ITEM_IMAGE, picturePath);
            values.put(ItemEntry.COLUMN_ITEM_SUPPLIER, supplierString);
            values.put(ItemEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailString);
            if (mCurrentItemUri == null) {
                Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);
                if (newUri == null) {
                    Toast.makeText(this, R.string.editor_insert_item_failed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.editor_insert_item_successful) + newUri, Toast.LENGTH_SHORT).show();
                }
            } else {
                int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);
                if (rowsAffected == 0) {
                    Toast.makeText(this, getString(R.string.editor_update_item_failed), Toast.LENGTH_SHORT);
                } else {
                    Toast.makeText(this, getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // save item to database
                saveItem();

                // exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
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
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_IMAGE,
                ItemEntry.COLUMN_ITEM_SUPPLIER,
                ItemEntry.COLUMN_SUPPLIER_EMAIL};
        return new CursorLoader(this,
                mCurrentItemUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1)
            return;
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int pictureColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);
            int supplierColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER);
            int supplierEmailColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_SUPPLIER_EMAIL);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String price = cursor.getString(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String stringUri = cursor.getString(pictureColumnIndex);
            String stringSupplier = cursor.getString(supplierColumnIndex);
            String stringEmailSupplier = cursor.getString(supplierEmailColumnIndex);
            Uri uriData = Uri.parse(stringUri);

            // Update the vies on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(price);
            mQuantityEditText.setText(Integer.toString(quantity));
            mSupplierEmailEditText.setText(stringEmailSupplier);
            mSupplierEditText.setText(stringSupplier);
            pictureUri = uriData;
            if (pictureUri.toString().contains("drawable"))
                mAddImage.setImageURI(uriData);
            else {
                Bitmap bM = getBitmapFromUri(pictureUri, mAddImage);
                mAddImage.setImageBitmap(bM);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mAddImage.setImageResource(R.drawable.no_image);
        mSupplierEditText.setText("");
        mSupplierEmailEditText.setText("");
    }
}