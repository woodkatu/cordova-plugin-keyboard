package org.apache.cordova.labs.keyboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.inputmethod.InputMethodManager;
import android.view.View;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

public class Keyboard extends CordovaPlugin {
    int previousHeightDiff = 0;

    @Override
    protected void pluginInitialize() {
        View rootView = cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            //r will be populated with the coordinates of your view that area still visible.
            rootView.getWindowVisibleDisplayFrame(r);

            // cache properties for later use
            int rootViewHeight = rootView.getRootView().getHeight();
            int resultBottom = r.bottom;

            // calculate screen height differently for android versions >= 21: Lollipop 5.x, Marshmallow 6.x
            //http://stackoverflow.com/a/29257533/3642890 beware of nexus 5
            int screenHeight;

            if (Build.VERSION.SDK_INT >= 21) {
                Display display = cordova.getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                screenHeight = size.y;
            } else {
                screenHeight = rootViewHeight;
            }

            int heightDiff = screenHeight - resultBottom;

            int pixelHeightDiff = heightDiff;
            if (pixelHeightDiff > 100 && pixelHeightDiff != previousHeightDiff) { // if more than 100 pixels, its probably a keyboard...
                String data = "{ 'keyboardHeight': " + pixelHeightDiff + " }";
                webView.sendJavascript("cordova.fireWindowEvent('keyboardDidShow')");
            } else if (pixelHeightDiff != previousHeightDiff && (previousHeightDiff - pixelHeightDiff) > 100) {
                webView.sendJavascript("cordova.fireWindowEvent('keyboardDidHide')");
            }
            previousHeightDiff = pixelHeightDiff;
        });

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Activity activity = this.cordova.getActivity();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        View view;
        try {
            view = (View) webView.getClass().getMethod("getView").invoke(webView);
        } catch (Exception e) {
            view = (View) webView;
        }

        if ("show".equals(action)) {
            imm.showSoftInput(view, 0);
            callbackContext.success();
            return true;
        } else if ("hide".equals(action)) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            callbackContext.success();
            return true;
        }
        callbackContext.error(action + " is not a supported action");
        return false;
    }
}
