package dasarathi.devops.toolkit.gui.messages;

import com.intellij.DynamicBundle;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.PropertyKey;

public final class DevOpsToolKit {
  private static final String BUNDLE = "messages.DevOpsToolKit";
  private static final DynamicBundle INSTANCE = new DynamicBundle(DevOpsToolKit.class, BUNDLE);

  private DevOpsToolKit() {
    /*
     * Loading Props DevOpsToolKit.properties
     * */
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }

  public static Supplier<@Nls String> lazyMessage(
      @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getLazyMessage(key, params);
  }
}
