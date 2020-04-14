# Welcome to Distraction-Free Reading Glasses

## Running the Android App

To run the Android app, you'll need to have Android Studio Installed and an Android device handy. 

From there:
1. Open the cloned repository
1. Setup your device in developer mode and turn on USB debugging
1. Plug your device into your computer - make sure it's a regurla USB port (not 3.0)
1. In Android Studio, select you device from teh run options
1. Click the play button

## Running the Arduino Code Solo

No Android? No problem.

You can commuicate with the Arduino via Serial. 
1. Upload the code to your device
1. Open the serial logger
1. Set line endings to \n

Sending Signals:
* I'm connected -> "<1>"
* I'm disconnecting -> "<0>"

Interpreting Recieved Signals:
* I'm off <- "<0>"
* I'm on <- "<1>"
