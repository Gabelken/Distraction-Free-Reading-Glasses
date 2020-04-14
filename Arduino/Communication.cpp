#include <Arduino.h>

/**
 * Class to send and receive signals to and from an Android app.
 * 
 * Checks for connections from a phone and sends updates as required
 * 
 * Requres: 
 * - A bluetooth module
*/
class CommunicationHandler {
  private:
    // Storage for incoming data
    const byte numChars = 10;
    char receivedChars[10];
    byte ndx = 0; //Index of the next char in the buffer string
    
    // Settings for parsing incoming data
    const char startMarker = '<';
    const char endMarker = '>';
    boolean recvInProgress = false;
    
    // Lets the controller know a signal is read in and ready to be handled
    boolean newData = false;

    // Tracks whether there is a phone connected to the Arduino that needs to recieve updates
    bool isConnected = false;
    
    /**
      Read a line of data in from the serial port, one character at a time. ***Non-blacking - MUST be called from loop or other non-blocking function***
      Only read data between start and end markers to avoid unwanted behaviour.

      Results are stored in 'receivedChars'
    */
    void recvWithStartEndMarkers() {
      char rc; //Char read in

      if (Serial.available() > 0) {
        rc = Serial.read();

        // When recieving, accept any characters until the end marker
        if (recvInProgress == true) {
          if (rc != endMarker) { //Reading the string
            receivedChars[ndx] = rc;
            ndx++;
            if (ndx >= numChars) {
              ndx = numChars - 1;
            }
          }
          else { //Done receiving the string
            receivedChars[ndx] = '\0'; // terminate the string
            recvInProgress = false;
            ndx = 0;
            newData = true;
          }
        }

        else if (rc == startMarker) { //Start reading chars once the start char appears
          recvInProgress = true;
        }
      }
    }

    /**
      Parse data received over serial into something the program can use.
      
      Returns the receieved data as an integer.
    */
    int parseReadData() {
      int result = atoi(receivedChars);
      return result;
    }

  public:
    void setup() {
      Serial.begin(9600);
    }
    
    /**
     * Send an update to any connected device.
     * If a phone is currently connected, send a message over serial telling it whether the glasses are on or off
     * 
     * isOn {bool} the state of the glasses
     */
    void Send(bool isOn) {
      if (isConnected) {
        Serial.print('<'); //Use brackets so the app can know when the whole message has arrived, and that it came from the right place
        int msg = (isOn) ? 1 : 0; // Send 1 if on, or 0 if off
        Serial.print(msg);
        Serial.println('>');
      }
    }
    
    /**
     * Reads incoming messages from the serial port to see if a device has signalled that it's ready.
     * If a new device has connected, send it the current state.
     * 
     * isOn {bool} the state of the glasses
     * 
     * Returns true if a phone is connected and listening, else false
     */
    bool CheckForPhone(bool isOn) {
      //Read incoming bits
      recvWithStartEndMarkers(); 
      
      // Once a whole message has been recieved, handle it
      if(newData) {
        int control = parseReadData(); 
        if(control == 0) { // Phone has disconnected and no longer expects data
          isConnected = false;
        }
        else if(control == 1) { //Phone has connected and expects data. It needs to know the current state
          isConnected = true;
          Send(isOn);
        }
        newData = false; // Reset newData to start looking for a new message
      }
      return isConnected;
    }

};