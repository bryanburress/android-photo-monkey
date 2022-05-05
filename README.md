# Photo Monkey Android App

The Photo Monkey Android App allows for taking pictures, adding a description, and saving them to a custom directory.

## Getting Started

To build and install the project follow the steps below:

    1) Clone the repo.
    2) Open Android Studio, and then open the root directory of the cloned repo.
    3) Connect an Android Phone (make sure debugging is enabled on the device).
    4) Install and run the app by clicking the "Play" button in Android Studio.

### Prerequisites

Install Android Studio to work on this code.

### QR Code Scanner
 The QR code scanner is available for easier one-off configuration of Photo Monkey remote URL when MDM is not used. The QR code should hold the full URL to POST photos to. 

## Changelog
 ##### [0.2.5](https://github.com/chesapeaketechnology/android-photo-monkey/releases/tag/v0.2.5) - 2022-05-05
  * Add QR code scanner for the remote URL setting

 ##### [0.2.0](https://github.com/chesapeaketechnology/android-photo-monkey/releases/tag/v0.2.0) - 2022-03-11
  * Add settings screen and allow for MDM management of settings
  * Upload photos to the optional Remote URL specified in the settings

##### [0.1.0](https://github.com/chesapeaketechnology/android-photo-monkey/releases/tag/v0.1.0) - 2020-07-08
 * Initial Release of the Photo Monkey app to take pictures, add a description, geo-tag, and push to Sync Monkey.
 * Reworked UI to be Fragment based
 * Added androidx navigation elements
 * Implemented support for using the Jetpack CameraX ImageCapture libraries
 * Implemented support for Capturing images to the MediaStore repository (public gallery - /Pictures)
 * Implemented support for Capturing images to the External Media Directory (Android/media)
 * Implemented support for reading and writing EXIF metadata for images from the MediaStore repository
 * Implemented support for reading and writing EXIF metadata for images from the External Media Directory
 * Implemented support for sharing images with SyncMonkey (manually)
 * Implemented support for automated sharing of images with SyncMonkey
 * Implemented support for deleting images
 * Implemented support for sharing images with other application registered for ACTION_SEND
 * Implemented location tracking support and stamping images with the location taken
 * Added compile time feature flags 
 * Modified colors to match corporate theme
 * Added Auto Focus, Auto White Balance, and Auto Exposure support
 * Added Tap to Focus support
 * Added Pinch to Zoom Support
 * Added Flash support (Auto, On, & Off)
 * Added automated removal of temp files

## Notes
#### View Exif Data on CLI
```exiftool -a -u -g1 ~/Downloads/PM_2020-05-28-18-14-55-624.jpg```

*exiftool can be installed via homebrew package manager*

#### Known Issues
 * Needs unit tests
 * There is an issue with writing to the MediaStore ContentResolver output stream for Android R (api 30)


## Contact
* **Christian Rowlands** - [Craxiom](https://github.com/christianrowlands)  
* **Les Stroud** - [lstroud](https://github.com/lstroud)  
