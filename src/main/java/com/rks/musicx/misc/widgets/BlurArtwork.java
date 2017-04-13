package com.rks.musicx.misc.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.widget.ImageView;

/*
 * Created by Coolalien on 6/28/2016.
 */

public class BlurArtwork extends AsyncTask<String, Void, String> {

    private Context context;
    private int radius;
    private Bitmap bitmap;
    private ImageView imageView;
    private Bitmap finalResult;
    private final int height;
    private final int width;

    public BlurArtwork(Context context, int radius, Bitmap bitmap, ImageView imageView, float scale) {
        this.context = context;
        this.radius = radius;
        this.bitmap = bitmap;
        this.imageView = imageView;
        height = Math.round(bitmap.getHeight() * scale);
        width = Math.round(bitmap.getWidth() * scale);
    }

    @Override
    protected String doInBackground(String... params) {
        finalResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        RenderScript renderScript = RenderScript.create(context); //rs initialized
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));//start blur
        Allocation allocationIn = Allocation.createFromBitmap(renderScript, bitmap); //blurred bitmap
        Allocation allocationOut = Allocation.createFromBitmap(renderScript, finalResult); //output bitmap
        scriptIntrinsicBlur.setRadius(radius); //radius option from users
        scriptIntrinsicBlur.setInput(allocationIn);
        scriptIntrinsicBlur.forEach(allocationOut);
        allocationOut.copyTo(finalResult);
        return "Executed";
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        imageView.setImageBitmap(finalResult);
    }

}


