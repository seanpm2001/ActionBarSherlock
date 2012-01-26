package com.actionbarsherlock;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import java.util.HashMap;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.internal.ActionBarSherlockNative;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuItemImpl;
import com.actionbarsherlock.internal.view.menu.MenuPresenter;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * <p>Helper for implementing the action bar design pattern across all versions
 * of Android.</p>
 *
 * <p>This class will manage interaction with a custom action bar based on the
 * Android 4.0 source code. The exposed API mirrors that of its native
 * counterpart and you should refer to its documentation for instruction.</p>
 *
 * @author Jake Wharton <jakewharton@gmail.com>
 * @version 4.0.0
 */
public abstract class ActionBarSherlock {
    protected static final String TAG = "ActionBarSherlock";
    protected static final boolean DEBUG = true;


    /** Activity interface for menu creation callback. */
    public interface OnCreatePanelMenuListener {
        public boolean onCreatePanelMenu(int featureId, Menu menu);
    }
    /** Activity interface for menu item selection callback. */
    public interface OnMenuItemSelectedListener {
        public boolean onMenuItemSelected(int featureId, MenuItem item);
    }
    /** Activity interface for menu preparation callback. */
    public interface OnPreparePanelListener {
        public boolean onPreparePanel(int featureId, View view, Menu menu);
    }
    /** Activity interface for action mode finished callback. */
    public interface OnActionModeFinishedListener {
        public void onActionModeFinished(ActionMode mode);
    }
    /** Activity interface for action mode started callback. */
    public interface OnActionModeStartedListener {
        public void onActionModeStarted(ActionMode mode);
    }



    /**
     * Wrap an existing activity with a custom action bar implementation.
     *
     * @param activity Activity to wrap.
     * @return Instance to interact with the action bar.
     */
    public static ActionBarSherlock wrap(Activity activity) {
        return wrap(activity, false);
    }

    /**
     * Act as a delegate for another class which is providing the services
     * of an action bar along with its normal responsibility.
     *
     * @param activity Owning activity.
     * @return Instance to interact with the action bar.
     */
    public static ActionBarSherlock wrap(Activity activity, boolean asDelegate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new ActionBarSherlockNative(activity, asDelegate);
        } else {
            return new ActionBarSherlockCompat(activity, asDelegate);
        }
    }


    /** Implementation which backs the action bar interface API. */
    protected ActionBar mActionBarPublic;

    /** Activity which is displaying the action bar. Also used for context. */
    protected final Activity mActivity;
    /** Whether delegating actions for the activity or managing ourselves. */
    protected final boolean mIsDelegate;

    /** Whether or not the title is stable and can be displayed. */
    protected boolean mIsTitleReady = false;

    /** Reference to our custom menu inflater which supports action items. */
    protected MenuInflater mMenuInflater;
    /** Current menu instance for managing action items. */
    protected MenuBuilder mMenu;
    /** Map between native options items and sherlock items (pre-3.0 only). */
    protected HashMap<android.view.MenuItem, MenuItemImpl> mNativeItemMap;
    /** Result of the last dispatch of menu creation. */
    protected boolean mLastCreateResult;
    /** Result of the last dispatch of menu preparation. */
    protected boolean mLastPrepareResult;


    /** Action bar menu-related callbacks. */
    private final MenuPresenter.Callback mMenuPresenterCallback = new MenuPresenter.Callback() {
        @Override
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            // TODO Auto-generated method stub
        }
    };

    /** Menu callbacks triggered with actions on our items. */
    private final MenuBuilder.Callback mMenuBuilderCallback = new MenuBuilder.Callback() {
        @Override
        public void onMenuModeChange(MenuBuilder menu) {
            reopenMenu(true);
        }

        @Override
        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            return dispatchOptionsItemSelected(item);
        }
    };

    /** Native menu item callback which proxies to our callback. */
    private final android.view.MenuItem.OnMenuItemClickListener mNativeItemListener = new android.view.MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            if (DEBUG) Log.d(TAG, "[mNativeItemListener.onMenuItemClick] item: " + item);

            final MenuItemImpl sherlockItem = mNativeItemMap.get(item);
            if (sherlockItem != null) {
                sherlockItem.invoke();
            } else {
                Log.e(TAG, "Options item \"" + item + "\" not found in mapping");
            }

            return true; //Do not allow continuation of native handling
        }
    };



    protected ActionBarSherlock(Activity activity, boolean isDelegateOnly) {
        if (DEBUG) Log.d(TAG, "[<ctor>] activity: " + activity + ", isDelegateOnly: " + isDelegateOnly);

        mActivity = activity;
        mIsDelegate = isDelegateOnly;
    }


    /**
     * Get the current action bar instance.
     *
     * @return Action bar instance.
     */
    public ActionBar getActionBar() {
        if (DEBUG) Log.d(TAG, "[getActionBar]");

        initActionBar();
        return mActionBarPublic;
    }

    protected abstract void initActionBar();


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle and interaction callbacks when delegating
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Notify action bar of a configuration change event. Should be dispatched
     * after the call to the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * public void onConfigurationChanged(Configuration newConfig) {
     *     super.onConfigurationChanged(newConfig);
     *     mSherlock.dispatchConfigurationChanged(newConfig);
     * }
     * </pre></blockquote>
     *
     * @param newConfig The new device configuration.
     */
    public void dispatchConfigurationChanged(Configuration newConfig) {}

    /**
     * Notify the action bar that the activity has finished its resuming. This
     * should be dispatched after the call to the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * protected void onPostResume() {
     *     super.onPostResume();
     *     mSherlock.dispatchPostResume();
     * }
     * </pre></blockquote>
     */
    public void dispatchPostResume() {}

    /**
     * Notify the action bar that the activity is pausing. This should be
     * dispatched before the call to the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * protected void onPause() {
     *     mSherlock.dispatchPause();
     *     super.onPause();
     * }
     * </pre></blockquote>
     */
    public void dispatchPause() {}

    /**
     * Notify the action bar that the activity is stopping. This should be
     * called before the superclass implementation.
     *
     * <blockquote><p>
     * @Override
     * protected void onStop() {
     *     mSherlock.dispatchStop();
     *     super.onStop();
     * }
     * </p></blockquote>
     */
    public void dispatchStop() {}

    /**
     * Indicate that the menu should be recreated by calling
     * {@link OnCreateOptionsMenuListener#onCreateOptionsMenu(com.actionbarsherlock.view.Menu)}.
     */
    public void dispatchInvalidateOptionsMenu() {
        if (DEBUG) Log.d(TAG, "[dispatchInvalidateOptionsMenu]");

        if (mMenu == null) {
            Context context = mActivity;
            if (mActionBarPublic != null) {
                TypedValue outValue = new TypedValue();
                mActivity.getTheme().resolveAttribute(R.attr.actionBarWidgetTheme, outValue, true);
                if (outValue.resourceId != 0) {
                    //We are unable to test if this is the same as our current theme
                    //so we just wrap it and hope that if the attribute was specified
                    //then the user is intentionally specifying an alternate theme.
                    context = new ContextThemeWrapper(context, outValue.resourceId);
                }
            }
            mMenu = new MenuBuilder(context);
            mMenu.setCallback(mMenuBuilderCallback);
        }

        mMenu.stopDispatchingItemsChanged();
        mMenu.clear();

        if (!dispatchCreateOptionsMenu()) {
            if (mActionBarPublic != null) {
                setMenu(null, mMenuPresenterCallback);
            }
            return;
        }

        if (!dispatchPrepareOptionsMenu()) {
            if (mActionBarPublic != null) {
                setMenu(null, mMenuPresenterCallback);
            }
            mMenu.startDispatchingItemsChanged();
            return;
        }

        //TODO figure out KeyEvent? See PhoneWindow#preparePanel
        KeyCharacterMap kmap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        mMenu.setQwertyMode(kmap.getKeyboardType() != KeyCharacterMap.NUMERIC);
        mMenu.startDispatchingItemsChanged();

        setMenu(mMenu, mMenuPresenterCallback);
    }
    
    protected abstract void setMenu(Menu menu, MenuPresenter.Callback cb);

    /**
     * Notify the action bar that it should display its overflow menu if it is
     * appropriate for the device. The implementation should conditionally
     * call the superclass method only if this method returns {@code false}.
     *
     * <blockquote><p>
     * @Override
     * public void openOptionsMenu() {
     *     if (!mSherlock.dispatchOpenOptionsMenu()) {
     *         super.openOptionsMenu();
     *     }
     * }
     * </p></blockquote>
     *
     * @return {@code true} if the opening of the menu was handled internally.
     */
    public boolean dispatchOpenOptionsMenu() {
        return false;
    }

    /**
     * Notify the action bar that it should close its overflow menu if it is
     * appropriate for the device. This implementation should conditionally
     * call the superclass method only if this method returns {@code false}.
     *
     * <blockquote><pre>
     * @Override
     * public void closeOptionsMenu() {
     *     if (!mSherlock.dispatchCloseOptionsMenu()) {
     *         super.closeOptionsMenu();
     *     }
     * }
     * </pre></blockquote>
     *
     * @return {@code true} if the closing of the menu was handled internally.
     */
    public boolean dispatchCloseOptionsMenu() {
        return false;
    }

    /**
     * Notify the class that the activity has finished its creation. This
     * should be called after the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * protected void onPostCreate(Bundle savedInstanceState) {
     *     mSherlock.dispatchPostCreate(savedInstanceState);
     *     super.onPostCreate(savedInstanceState);
     * }
     * </pre></blockquote>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle
     *                           contains the data it most recently supplied in
     *                           {@link Activity#}onSaveInstanceState(Bundle)}.
     *                           <strong>Note: Otherwise it is null.</strong>
     */
    public void dispatchPostCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "[dispatchOnPostCreate]");

        if (mIsDelegate) {
            mIsTitleReady = true;
        }
    }

    /**
     * Notify the action bar that the title has changed and the action bar
     * should be updated to reflect the change. This should be called before
     * the superclass implementation.
     *
     * <blockquote><pre>
     *  @Override
     *  protected void onTitleChanged(CharSequence title, int color) {
     *      mSherlock.dispatchTitleChanged(title, color);
     *      super.onTitleChanged(title, color);
     *  }
     * </pre></blockquote>
     *
     * @param title New activity title.
     * @param color New activity color.
     */
    public void dispatchTitleChanged(CharSequence title, int color) {}

    /**
     * Notify the action bar that the user has pressed a key. This is used to
     * toggle the display of the overflow action item should one be forced on
     * a device with a menu key.
     *
     * <blockquote><pre>
     *  @Override
     *  public boolean onKeyUp(int keyCode, KeyEvent event) {
     *      if (mSherlock.dispatchKeyUp(keyCode, event)) {
     *          return true;
     *      }
     *      return super.onKeyDown(keyCode, event);
     *  }
     * </pre></blockquote>
     *
     * @param keyCode The value in event.getKeyCode().
     * @param event Description of the key event.
     * @return {@code true} if the event was handled.
     */
    public boolean dispatchKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Internal method to trigger the menu creation process.
     *
     * @return {@code true} if menu creation should proceed.
     */
    private boolean dispatchCreateOptionsMenu() {
        if (DEBUG) Log.d(TAG, "[dispatchCreateOptionsMenu]");

        mLastCreateResult = false;
        if (mActivity instanceof OnCreatePanelMenuListener) {
            OnCreatePanelMenuListener listener = (OnCreatePanelMenuListener)mActivity;
            mLastCreateResult = listener.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, mMenu);
        }
        return mLastCreateResult;
    }

    /**
     * Internal method to trigger the menu preparation process.
     *
     * @return {@code true} if menu preparation should proceed.
     */
    protected boolean dispatchPrepareOptionsMenu() {
        if (DEBUG) Log.d(TAG, "[dispatchPrepareOptionsMenu]");

        mLastPrepareResult = false;
        if (mActivity instanceof OnPreparePanelListener) {
            OnPreparePanelListener listener = (OnPreparePanelListener)mActivity;
            mLastPrepareResult = listener.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, mMenu);
        }
        return mLastPrepareResult;
    }

    /**
     * Notify the action bar that the Activity has triggered a menu preparation
     * which usually means that the user has requested the overflow menu via a
     * hardware menu key. You should return the result of this method call and
     * not call the superclass implementation.
     *
     * <blockquote><p>
     * @Override
     * public final boolean onPrepareOptionsMenu(android.view.Menu menu) {
     *     return mSherlock.dispatchPrepareOptionsMenu(menu);
     * }
     * </p></blockquote>
     *
     * @param menu Activity native menu
     * @return {@code true} if menu display should proceed.
     */
    public boolean dispatchPrepareOptionsMenu(android.view.Menu menu) {
        if (DEBUG) Log.d(TAG, "[dispatchPrepareOptionsMenu] android.view.Menu: " + menu);

        if (!dispatchPrepareOptionsMenu()) {
            return false;
        }

        /* TODO if (isReservingOverflow()) {
            return false;
        }*/

        if (mNativeItemMap == null) {
            mNativeItemMap = new HashMap<android.view.MenuItem, MenuItemImpl>();
        } else {
            mNativeItemMap.clear();
        }

        if (mMenu == null) {
        	return false;
        }
        
        return mMenu.bindNativeOverflow(menu, mNativeItemListener, mNativeItemMap);
    }

    /**
     * Internal method for dispatching options menu selection to the owning
     * activity callback.
     *
     * @param item Selected options menu item.
     * @return {@code true} if the item selection was handled in the callback.
     */
    protected boolean dispatchOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "[dispatchOptionsItemSelected] item: " + item);

        if (mActivity instanceof OnMenuItemSelectedListener) {
            OnMenuItemSelectedListener listener = (OnMenuItemSelectedListener)mActivity;
            return listener.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
        }
        return false;
    }

    /**
     * Notify the action bar that the overflow menu has been opened. The
     * implementation should conditionally return {@code true} if this method
     * returns {@code true}, otherwise return the result of the superclass
     * method.
     *
     * <blockquote><p>
     * @Override
     * public final boolean onMenuOpened(int featureId, android.view.Menu menu) {
     *     if (mSherlock.dispatchMenuOpened(featureId, menu)) {
     *         return true;
     *     }
     *     return super.onMenuOpened(featureId, menu);
     * }
     * </p></blockquote>
     *
     * @param featureId Window feature which triggered the event.
     * @param menu Activity native menu.
     * @return {@code true} if the event was handled by this method.
     */
    public boolean dispatchMenuOpened(int featureId, android.view.Menu menu) {
        return false;
    }

    /**
     * Notify the action bar that the overflow menu has been closed. This
     * method should be called before the superclass implementation.
     *
     * <blockquote><p>
     * @Override
     * public void onPanelClosed(int featureId, android.view.Menu menu) {
     *     mSherlock.dispatchPanelClosed(featureId, menu);
     *     super.onPanelClosed(featureId, menu);
     * }
     * </p></blockquote>
     *
     * @param featureId
     * @param menu
     */
    public void dispatchPanelClosed(int featureId, android.view.Menu menu){}

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Return the feature bits that are enabled. This is the set of features
     * that were given to requestFeature(), and are being handled by this
     * Window itself or its container. That is, it is the set of requested
     * features that you can actually use.
     *
     * @return The feature bits.
     */
    public abstract int getFeatures();

    /**
     * Query for the availability of a certain feature.
     *
     * @param featureId The feature ID to check.
     * @return {@code true} if feature is enabled, {@code false} otherwise.
     */
    public abstract boolean hasFeature(int featureId);

    /**
     * Enable extended screen features. This must be called before
     * {@code setContentView()}. May be called as many times as desired as long
     * as it is before {@code setContentView()}. If not called, no extended
     * features will be available. You can not turn off a feature once it is
     * requested.
     *
     * @param featureId The desired features, defined as constants by Window.
     * @return Returns true if the requested feature is supported and now
     * enabled.
     */
    public abstract boolean requestFeature(int featureId);

    /**
     * Set extra options that will influence the UI for this window.
     *
     * @param uiOptions Flags specifying extra options for this window.
     */
    public abstract void setUiOptions(int uiOptions);

    /**
     * Set extra options that will influence the UI for this window. Only the
     * bits filtered by mask will be modified.
     *
     * @param uiOptions Flags specifying extra options for this window.
     * @param mask Flags specifying which options should be modified. Others
     *             will remain unchanged.
     */
    public abstract void setUiOptions(int uiOptions, int mask);

    /**
     * Set the content of the activity inside the action bar.
     *
     * @param layoutResId Layout resource ID.
     */
    public abstract void setContentView(int layoutResId);

    /**
     * Set the content of the activity inside the action bar.
     *
     * @param view The desired content to display.
     */
    public void setContentView(View view) {
        if (DEBUG) Log.d(TAG, "[setContentView] view: " + view);

        setContentView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    /**
     * Set the content of the activity inside the action bar.
     *
     * @param view The desired content to display.
     * @param params Layout parameters to apply to the view.
     */
    public abstract void setContentView(View view, ViewGroup.LayoutParams params);

    /**
     * Variation on {@link #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)}
     * to add an additional content view to the screen. Added after any
     * existing ones on the screen -- existing views are NOT removed.
     *
     * @param view The desired content to display.
     * @param params Layout parameters for the view.
     */
    public abstract void addContentView(View view, ViewGroup.LayoutParams params);

    /**
     * Change the title associated with this activity.
     */
    public void setTitle(CharSequence title) {
        if (DEBUG) Log.d(TAG, "[setTitle] title: " + title);

        dispatchTitleChanged(title, 0);
    }

    /**
     * Change the title associated with this activity.
     */
    public void setTitle(int resId) {
        if (DEBUG) Log.d(TAG, "[setTitle] resId: " + resId);

        setTitle(mActivity.getString(resId));
    }

    /**
     * Sets the visibility of the progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public abstract void setProgressBarVisibility(boolean visible);

    /**
     * Sets the visibility of the indeterminate progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public abstract void setProgressBarIndeterminateVisibility(boolean visible);

    /**
     * Sets whether the horizontal progress bar in the title should be indeterminate (the circular
     * is always indeterminate).
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param indeterminate Whether the horizontal progress bar should be indeterminate.
     */
    public abstract void setProgressBarIndeterminate(boolean indeterminate);

    /**
     * Sets the progress for the progress bars in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param progress The progress for the progress bar. Valid ranges are from
     *            0 to 10000 (both inclusive). If 10000 is given, the progress
     *            bar will be completely filled and will fade out.
     */
    public abstract void setProgress(int progress);

    /**
     * Sets the secondary progress for the progress bar in the title. This
     * progress is drawn between the primary progress (set via
     * {@link #setProgress(int)} and the background. It can be ideal for media
     * scenarios such as showing the buffering progress while the default
     * progress shows the play progress.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param secondaryProgress The secondary progress for the progress bar. Valid ranges are from
     *            0 to 10000 (both inclusive).
     */
    public abstract void setSecondaryProgress(int secondaryProgress);


    /**
     * Get a menu inflater instance which supports the newer menu attributes.
     *
     * @return Menu inflater instance.
     */
    public MenuInflater getMenuInflater() {
        if (DEBUG) Log.d(TAG, "[getMenuInflater]");

        // Make sure that action views can get an appropriate theme.
        if (mMenuInflater == null) {
            initActionBar();
            if (mActionBarPublic != null) {
                mMenuInflater = new MenuInflater(getThemedContext());
            } else {
                mMenuInflater = new MenuInflater(mActivity);
            }
        }
        return mMenuInflater;
    }
    
    protected abstract Context getThemedContext();
    
    protected void reopenMenu(boolean toggleMenuMode) {}

    /**
     * Start an action mode.
     *
     * @param callback Callback that will manage lifecycle events for this
     *                 context mode.
     * @return The ContextMode that was started, or null if it was canceled.
     * @see ActionMode
     */
    public abstract ActionMode startActionMode(ActionMode.Callback callback);
}