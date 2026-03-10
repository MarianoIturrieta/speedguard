import sys

path = "android/app/src/main/AndroidManifest.xml"
with open(path, "r") as f:
    content = f.read()

perms = (
    '\n    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />'
    '\n    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />'
    '\n    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />'
    '\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />'
    '\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />'
    '\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />'
    '\n    <uses-permission android:name="android.permission.WAKE_LOCK" />'
)

service = (
    '\n        <service'
    '\n            android:name=".LocationService"'
    '\n            android:enabled="true"'
    '\n            android:exported="false"'
    '\n            android:foregroundServiceType="location" />'
)

if "<application" not in content:
    print("ERROR: <application not found in manifest")
    sys.exit(1)

content = content.replace("<application", perms + "\n\n    <application", 1)
content = content.replace("</application>", service + "\n    </application>", 1)

with open(path, "w") as f:
    f.write(content)

print("Manifest updated OK")
print(content)
