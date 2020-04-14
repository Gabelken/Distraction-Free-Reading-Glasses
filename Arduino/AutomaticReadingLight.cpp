#include <Arduino.h>

/**
 * Class to control light-sensitive reading lights situated on the glasses.
 * 
 * If the glasses are on, reads the light level and turns LEDs on if the area is too dim. Increases in brightness as the room gets dimmer.
 * 
 * Requres: 
 * - Any number of LEDs connected in parallel to a PMW pin through a resistor
 * - A photocell connected to an analog pin
*/
class AutomaticReadingLight {
  private:
    // Pins used
    const byte ledPin;
    const byte sensorPin;
    
    //Tuning values for the photocell
    const int threshold = 100; // Sensor threshold at which the room is deemed 'too dim'
    const int minBrightness = 80; //Lowest brightness setting of the LEDs
    float brightnessScale; // Scale at which leds brighten or dim to adjust to the ambient light level. Set based on the tuned values.
    
  public:
    // Constructor - Set pins and setup brightness scale value based on tuning values
    AutomaticReadingLight(byte ledPin, byte sensorPin) : ledPin(ledPin), sensorPin(sensorPin) {
      //Brightness is adjusted to be between minBrightness and 255, based on distance from the threshold
      brightnessScale = (255-minBrightness);
      brightnessScale /= threshold;
    }
    
    void setup() {
      pinMode(ledPin, OUTPUT);
      pinMode(sensorPin, INPUT);
    }
    
    // Function to be called every time the program loops
    void loop(bool isOn) {
      if(isOn) {
        // Check the ambient light level
        int lightLevel = analogRead(sensorPin);
        
        // If the light level is dimmer than the tuned threshold, turn the lights on
        if(lightLevel <= threshold) {
          
          // Find how far the light level is from the threshold
          float brightnessDiff = threshold-lightLevel;
          
          // Increase brightness based on how much worse the reading is than the threshold.
          int scaledBrightnessDiff = (int)(brightnessDiff*brightnessScale);
          int brightness = scaledBrightnessDiff + minBrightness;
          
          // Set the LED(s)
          analogWrite(ledPin, brightness);
          return;
        }
      }
      // If the glasses are off, or the area is bright enough, turn off the LED(s)
      analogWrite(ledPin, 0);
    }
  
};