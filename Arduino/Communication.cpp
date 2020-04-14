#include <Arduino.h>

class CommunicationHandler {
  private:
    const byte numChars = 10;
    char receivedChars[10];
    
    const char startMarker = '<';
    const char endMarker = '>';
    boolean recvInProgress = false;
    byte ndx = 0; //Index of the next char in the buffer string
    
    boolean newData = false;

    bool isConnected = false;
    
    int testLED_R = 46;
    int testLED_St = 42;
    int testLED_G = 38;
    int testLED_En = 34;
    
    /**
      Source: https://forum.arduino.cc/index.php?topic=396450
      Read a line of data in from the serial port, one character at a time. ***Non-blacking - MUST be called from loop***
      Only read data between start and end markers to avoid unwanted behaviour.

      Results are stored in 'receivedChars'.
    */
    void recvWithStartEndMarkers() {
      char rc; //Char read in

      if (Serial.available() > 0){// && newData == false) {
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
    */
    int parseReadData() {
      int result = atoi(receivedChars);
      return result;
    }

  public:
    void setup() {
      Serial.begin(9600);
    }

    void Send(bool isOn) {
      if (isConnected) {
        Serial.print('<');
        int msg = (isOn) ? 1 : 0;
        Serial.print(msg);
        Serial.println('>');
      }
    }
    
    bool CheckForPhone(bool isOn) {
      recvWithStartEndMarkers();
      if(newData) {
        int control = parseReadData();
        if(control == 0) {
          isConnected = false;
        }
        else if(control == 1) {
          isConnected = true;
        }
        Send(isOn);
        newData = false;
      }
      return isConnected;
    }

};