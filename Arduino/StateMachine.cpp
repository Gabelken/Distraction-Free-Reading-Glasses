#include <Arduino.h>/** * Class to keep track of the on/off state of the glasses *  * Checks if the pressure sensor has been tripped to determine if the glasses are being worn or not. *  * Requres:  * - A pressure sensor connected to an analog pin */class DualStateMachine {    using Callback = void (*)(const bool);  private:    // Sensor pin    const byte switchPin;        // Current state of the system    bool isOn = false;        //Tuning value for the pressure sesnsor    const int threshold = 800;      public:    // Constructor - Set pin    DualStateMachine(byte pin) : switchPin(pin) {}    void setup() {      pinMode(switchPin, INPUT);      CheckState([]{}); // Set the initial state    }        /**     * Check if the glasses are being worn and update the system's stored state accordingly.     * On a change to a new state, handle an onStateChanged event function passed in by the caller.     *      * onStateChanged {Callback} A function to be called when the state of the system changes     *      * Returns the current state of the system     */    bool CheckState(Callback onStateChanged) {      // Read input from pressure sensor      int reading = analogRead(switchPin);      // Get teh new state      bool newState = reading >= threshold;            //If the state has changed, update the stored state and do an action defined by the caller      if (newState != isOn) {        isOn = newState;        onStateChanged(isOn);      }      return isOn;    }        /**     * // Testing function //     * Check the current stored state without updating the stored state     */    bool GetState() {      return isOn;    }};