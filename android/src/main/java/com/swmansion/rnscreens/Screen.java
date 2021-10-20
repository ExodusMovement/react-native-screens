package com.swmansion.rnscreens;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.os.Parcelable;
import android.util.SparseArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.facebook.react.uimanager.PointerEvents;
import com.facebook.react.uimanager.ReactPointerEventsView;
import java.lang.reflect.Field;

public class Screen extends ViewGroup implements ReactPointerEventsView {

  public static class ScreenFragment extends Fragment {

    private Screen mScreenView;

    public ScreenFragment() {
      throw new IllegalStateException("Screen fragments should never be restored");
    }

    @SuppressLint("ValidFragment")
    public ScreenFragment(Screen screenView) {
      super();
      mScreenView = screenView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
      return mScreenView;
    }
  }

  private final Fragment mFragment;
  private @Nullable ScreenContainer mContainer;
  private boolean mActive;
  private boolean mTransitioning;

  public Screen(Context context) {
    super(context);
    mFragment = new ScreenFragment(this);
  }

  @Override
  protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
    // no-op
  }

  /**
   * While transitioning this property allows to optimize rendering behavior on Android and provide
   * a correct blending options for the animated screen. It is turned on automatically by the container
   * when transitioning is detected and turned off immediately after
   */
  public void setTransitioning(boolean transitioning) {
    if (mTransitioning == transitioning) {
      return;
    }
    mTransitioning = transitioning;
    boolean isWebViewInScreen = hasWebView(this);
    if (isWebViewInScreen && getLayerType() != View.LAYER_TYPE_HARDWARE) {
      return;
    }
    super.setLayerType(transitioning && !isWebViewInScreen ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE, null);
  }

  private boolean hasWebView(ViewGroup viewGroup) {
    for(int i = 0; i < viewGroup.getChildCount(); i++) {
      View child = viewGroup.getChildAt(i);
      if (child instanceof WebView) {
        return true;
      } else if (child instanceof ViewGroup) {
        if (hasWebView((ViewGroup) child)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
    // do nothing, react native will keep the view hierarchy so no need to serialize/deserialize
    // view's states. The side effect of restoring is that TextInput components would trigger set-text
    // events which may confuse text input handling.
  }

  @Override
  protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
    // ignore restoring instance state too as we are not saving anything anyways.
  }

  @Override
  public boolean hasOverlappingRendering() {
    return mTransitioning;
  }

  @Override
  public PointerEvents getPointerEvents() {
    return mTransitioning ? PointerEvents.NONE : PointerEvents.AUTO;
  }

  @Override
  public void setLayerType(int layerType, @Nullable Paint paint) {
    // ignore – layer type is controlled by `transitioning` prop
  }

  public void setNeedsOffscreenAlphaCompositing(boolean needsOffscreenAlphaCompositing) {
    // ignore - offscreen alpha is controlled by `transitioning` prop
  }

  protected void setContainer(@Nullable ScreenContainer mContainer) {
    // source:
    // explanation:
    //   https://github.com/kmagiera/react-native-screens/issues/54#issuecomment-475091756
    //   https://github.com/kmagiera/react-native-screens/issues/54#issuecomment-475355506
    // code:
    //   https://github.com/TikiTDO/react-native-screens/commit/5e490b22c1e04b558816437f9fd2aa723e393fe6
    this.mContainer = mContainer;
    if (mContainer == null) {
      try {
        Field f = mFragment.getClass().getSuperclass().getDeclaredField("mContainerId");
        f.setAccessible(true);
        f.set(this.mFragment, 0);
      } catch(NoSuchFieldException e) {
        // Eat the error, nom nom
      } catch(IllegalAccessException e) {
        // This one too. It is delicious.
      }
    }
  }

  protected @Nullable ScreenContainer getContainer() {
    return mContainer;
  }

  protected Fragment getFragment() {
    return mFragment;
  }

  public void setActive(boolean active) {
    if (active == mActive) {
      return;
    }
    mActive = active;
    if (mContainer != null) {
      mContainer.notifyChildUpdate();
    }
  }

  public boolean isActive() {
    return mActive;
  }
}
