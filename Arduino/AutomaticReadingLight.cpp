#include <Arduino.h>

class AutomaticReadingLight {
  private:
    const byte ledPin;
    const byte sensorPin;
    const int threshold = 100;
    const int minBrightness = 80;
    float brightnessScale;
    
  public:
    AutomaticReadingLight(byte ledPin, byte sensorPin) : ledPin(ledPin), sensorPin(sensorPin) {
      brightnessScale = (255-minBrightness);
      brightnessScale /= threshold;
    }
    
    void setup() {
      pinMode(ledPin, OUTPUT);
      pinMode(sensorPin, INPUT);
    }
    
    void loop(bool isOn) {
      if(isOn) {
        int lightLevel = analogRead(sensorPin);
        if(lightLevel <= threshold) {
          
          // Calc 
          float brightnessDiff = threshold-lightLevel;

          int scaledBrightnessDiff = (int)(brightnessDiff*brightnessScale);
          int brightness = scaledBrightnessDiff + minBrightness;
          
          analogWrite(ledPin, brightness);
          return;
        }
      }
      analogWrite(ledPin, 0);
    }
  
};