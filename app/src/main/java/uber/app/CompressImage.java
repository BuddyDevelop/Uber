package uber.app;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CompressImage {
    public static ByteArrayOutputStream compressImage( Context context, Uri uri ) {

        String filePath = getRealPathFromURI( context, uri );
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory.
//      Just the bounds are loaded. If you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile( filePath, options );

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 120x120
        float maxHeight = 1500.0f;
        float maxWidth = 1500.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image
        if ( actualHeight > maxHeight || actualWidth > maxWidth ) {
            if ( imgRatio < maxRatio ) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = ( int ) ( imgRatio * actualWidth );
                actualHeight = ( int ) maxHeight;
            } else if ( imgRatio > maxRatio ) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = ( int ) ( imgRatio * actualHeight );
                actualWidth = ( int ) maxWidth;
            } else {
                actualHeight = ( int ) maxHeight;
                actualWidth = ( int ) maxWidth;

            }
        }

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[ 16 * 1024 ];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile( filePath, options );
        } catch ( OutOfMemoryError exception ) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap( actualWidth, actualHeight, Bitmap.Config.ARGB_8888 );
        } catch ( OutOfMemoryError exception ) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / ( float ) options.outWidth;
        float ratioY = actualHeight / ( float ) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale( ratioX, ratioY, middleX, middleY );

        Canvas canvas = new Canvas( scaledBitmap );
        canvas.setMatrix( scaleMatrix );
        canvas.drawBitmap( bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint( Paint.FILTER_BITMAP_FLAG ) );

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface( filePath );

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0 );
            Log.d( "EXIF", "Exif: " + orientation );
            Matrix matrix = new Matrix();
            if ( orientation == 6 ) {
                matrix.postRotate( 90 );
                Log.d( "EXIF", "Exif: " + orientation );
            } else if ( orientation == 3 ) {
                matrix.postRotate( 180 );
                Log.d( "EXIF", "Exif: " + orientation );
            } else if ( orientation == 8 ) {
                matrix.postRotate( 270 );
                Log.d( "EXIF", "Exif: " + orientation );
            }
            scaledBitmap = Bitmap.createBitmap( scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

//          write the compressed bitmap at the destination specified by filename.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(  );
        scaledBitmap.compress( Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream );

        return byteArrayOutputStream;
    }

    public static String getRealPathFromURI( Context context, Uri uri ) {
        Uri queryUri = MediaStore.Files.getContentUri( "external" );
        String columnData = MediaStore.Files.FileColumns.DATA;
        String columnSize = MediaStore.Files.FileColumns.SIZE;

        String[] projectionData = { MediaStore.Files.FileColumns.DATA };


        String name = null;
        String size = null;

        Cursor cursor = context.getContentResolver().query( uri, null, null, null, null );
        if ( ( cursor != null ) && ( cursor.getCount() > 0 ) ) {
            int nameIndex = cursor.getColumnIndex( OpenableColumns.DISPLAY_NAME );
            int sizeIndex = cursor.getColumnIndex( OpenableColumns.SIZE );

            cursor.moveToFirst();

            name = cursor.getString( nameIndex );
            size = cursor.getString( sizeIndex );

            cursor.close();
        }

        String imagePath = "";
        if ( ( name != null ) && ( size != null ) ) {
            String selectionNS = columnData + " LIKE '%" + name + "' AND " + columnSize + "='" + size + "'";

            Cursor cursorLike = context.getContentResolver().query( queryUri, projectionData, selectionNS, null, null );

            if ( ( cursorLike != null ) && ( cursorLike.getCount() > 0 ) ) {
                cursorLike.moveToFirst();
                int indexData = cursorLike.getColumnIndex( columnData );
                if ( cursorLike.getString( indexData ) != null ) {
                    imagePath = cursorLike.getString( indexData );
                }
                cursorLike.close();
            }
        }

        return imagePath;
    }
}
