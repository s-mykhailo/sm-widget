import sys, os
out = sys.argv[1]
FONTS = {"sans":"sans-serif","light":"sans-serif-light","thin":"sans-serif-thin","black":"sans-serif-black",
         "cond":"sans-serif-condensed","serif":"serif","mono":"monospace","cursive":"cursive"}
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
def frame(inner):
    G = CUR[0]
    return f'''<FrameLayout {NS}
    android:id="@+id/root" android:layout_width="match_parent" android:layout_height="match_parent">
    <ImageView android:id="@+id/bg" android:layout_width="match_parent" android:layout_height="match_parent"
        android:scaleType="fitXY" android:src="@drawable/bg_r2" android:tint="#E6202124" android:importantForAccessibility="no"/>
    <LinearLayout android:id="@+id/content" android:layout_width="match_parent" android:layout_height="match_parent"
        android:orientation="vertical" android:gravity="{G}" android:padding="14dp">
{inner}
    </LinearLayout>
</FrameLayout>
'''
def tv(id, fam, size, text, tag="TextView", extra=""):
    G = CUR[0]
    return f'''        <{tag} android:id="@+id/{id}" android:layout_width="match_parent" android:layout_height="wrap_content"
            android:fontFamily="{fam}" android:textSize="{size}sp" android:textColor="#FFFFFF" android:gravity="{G}"
            android:includeFontPadding="false" {extra} android:text="{text}"/>'''
CUR=["center"]
ALIGNS={"l":"start|center_vertical","c":"center","r":"end|center_vertical"}
for (key, fam), (ak, G) in [(f,a) for f in FONTS.items() for a in ALIGNS.items()]:
    CUR[0] = G
    L = {}
    L["clock"] = frame(
        tv("line1", fam, 56, "12:45", "TextClock", 'android:format12Hour="h:mm" android:format24Hour="HH:mm" android:maxLines="1"') + "\n" +
        tv("line2", fam, 16, "п\\'ятниця, 13 листопада", "TextClock", 'android:format12Hour="EEEE, d MMMM" android:format24Hour="EEEE, d MMMM" android:layout_marginTop="4dp" android:maxLines="1" android:ellipsize="end"'))
    L["text"] = frame(
        tv("line1", fam, 22, "Роби. Не чекай.", extra='android:maxLines="6" android:ellipsize="end"') + "\n" +
        tv("line2", fam, 13, "", extra='android:layout_marginTop="6dp" android:alpha="0.75" android:maxLines="2"'))
    L["battery"] = frame(f'''        <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
            <ImageView android:id="@+id/image" android:layout_width="match_parent" android:layout_height="match_parent"
                android:scaleType="fitCenter" android:importantForAccessibility="no"/>
            <TextView android:id="@+id/line1" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_gravity="center" android:fontFamily="{fam}" android:textSize="22sp" android:textColor="#FFFFFF"
                android:includeFontPadding="false" android:text="87%"/>
        </FrameLayout>
''' + tv("line2", fam, 12, "Батарея", extra='android:layout_marginTop="6dp" android:alpha="0.8" android:maxLines="1"'))
    L["progress"] = frame(
        tv("line1", fam, 16, "Рік 2026", extra='android:maxLines="1"') + '''
        <ProgressBar android:id="@+id/bar" style="@android:style/Widget.ProgressBar.Horizontal"
            android:layout_width="match_parent" android:layout_height="10dp" android:layout_marginTop="8dp" android:layout_marginBottom="8dp"
            android:max="1000" android:progress="870" android:progressDrawable="@drawable/progress_bar"
            android:progressTint="#FFFFFF" android:progressBackgroundTint="#40FFFFFF"/>
''' + tv("line2", fam, 13, "87%", extra='android:alpha="0.8" android:maxLines="1"'))
    L["countdown"] = frame(
        tv("line1", fam, 48, "49", extra='android:maxLines="1"') + "\n" +
        tv("line2", fam, 13, "днів до Нового року", extra='android:layout_marginTop="4dp" android:alpha="0.85" android:maxLines="2" android:ellipsize="end"'))
    for t, xml in L.items():
        open(os.path.join(out, f"w_{t}_{key}_{ak}.xml"), "w").write(xml)
print("done", len(FONTS)*5*3)
