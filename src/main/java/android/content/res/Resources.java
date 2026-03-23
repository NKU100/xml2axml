package android.content.res;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by Roy on 15-10-6.
 */
public class Resources {

    public HashMap<String, Integer> attrMap = new HashMap<>();

    /**
     * Hidden (@hide) platform attributes that are NOT in the public android.jar
     * but are commonly found in APK manifests (written by aapt2).
     * Resource IDs sourced from AOSP frameworks/base/core/res/res/values/public.xml
     */
    private static final Object[][] HIDDEN_ATTRS = {
        {"compileSdkVersion",           0x01010572},
        {"compileSdkVersionCodename",   0x01010573},
        {"appComponentFactory",         0x0101057a},
        {"usesNonSdkApi",               0x01010578},
        {"allowCrossUidActivitySwitchFromBelow", 0x01010680},
    };

    private void init() {
        try {
            Field[] fs = android.R.attr.class.getFields();
            for (Field f : fs) {
                attrMap.put(f.getName(), (Integer) f.get(null));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Add hidden platform attrs that are missing from public android.jar
        for (Object[] entry : HIDDEN_ATTRS) {
            attrMap.putIfAbsent((String) entry[0], (Integer) entry[1]);
        }
    }

    public Resources() {
        init();
    }

    public int getIdentifier(String name, String type, String pkg) {
        if ("android".equals(pkg) && "attr".equals(type)) {
            Integer x = attrMap.get(name);
            if (x == null) System.out.println("attr not found: " + name);
            else {
                return x;
            }
        }
        return 0;
    }
}
