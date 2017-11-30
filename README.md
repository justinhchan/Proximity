# Proximity
CMSC436 Group Project that uses Google Nearby API

## Setup

### Enable Nearby Messages

Setup your Nearby Messagees API at [Google Developers Console](https://console.developers.google.com/).  
Follow official instruction below.  
https://developers.google.com/nearby/messages/android/get-started

### Put API KEY in AndroidManifest

Put your API KEY in `android:value` of `meta-data` inside the `AndroidManifest.xml` file.


```xml
  ...
  <meta-data
    android:name="com.google.android.nearby.messages.API_KEY"
    android:value="[API_KEY]" />
  ...
```
