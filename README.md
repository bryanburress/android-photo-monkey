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

## Changelog
 
##### [0.1.0]() - 2020-05-28
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

## Notes
#### View Exif Data on CLI
```exiftool -a -u -g1 ~/Downloads/PM_2020-05-28-18-14-55-624.jpg```

*exiftool can be installed via homebrew package manager*

#### Known Issues
 * Needs unit tests
 * There is an issue with writing to the MediaStore ContentResolver output stream for Android R (api 30)


## Contact
* Christian Rowlands <crowlands@ctic-inc.com>  
* Les Stroud <lstroud@ctic-inc.com>  
