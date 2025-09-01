# Burp Window Title Controller

This is a Burp Suite extension built with the Montoya API.  
It allows you to control and customize the Burp Suite window title beyond the default display.

## Features

- Hide or show the ` - licensed to ...` suffix in the Burp Suite window title
- Set a custom window title of your choice
- Reset back to the original Burp default title
- Display the current window title in the "Title" menu

## Installing the Extension

1. Open Burp Suite.
2. Go to the "Extensions" tab.
3. Click on "Add".
4. Select "Java" as the extension type.
5. Choose the compiled JAR file (you can build it yourself with Gradle or use the one in `build/libs/Burp_Window_Title_Controller.jar`).

## Usage

Once the extension is loaded, a new **Title** menu will appear in the Burp Suite menu bar.  
From this menu you can:

- View the current window title (`Current: ...`)
- Toggle the visibility of the license suffix
- Set a custom title through a dialog prompt
- Reset the title back to Burp’s default

## Build

To build the JAR yourself:

```bash
./gradlew clean build
```
The compiled file will be placed in build/libs/.


## Acknowledgement

This project was inspired by [ronen1n](https://github.com/ronen1n)'s 
**Burp License Title Cleaner**, and extends it with additional features 
such as custom title setting and reset functionality.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.