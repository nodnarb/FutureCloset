#include <SPI.h>
#include <WiFi.h>

#include "M2XStreamClient.h"

char ssid[] = "NETGEAR93"; //  your network SSID (name)
char pass[] = "basiccream298";    // your network password (use for WPA, or use as key for WEP)
int keyIndex = 0;            // your network key Index number (needed only for WEP)

int status = WL_IDLE_STATUS;
// use the numeric IP instead of the name for the server:
char server[] = "192.168.1.4";    // name address server
int port = 9393;

char feedId[] = "37a1813b03d46e672b9040de6abf0f73";
char streamName[] = "temperature";
char m2xKey[] = "1e56f68dfd3fc3ad4f129facb6b831b4";

WiFiClient client;
M2XStreamClient m2xClient(&client, m2xKey, server, port);

void on_data_point_found(const char* at, const char* value, int index, void* context) {
  Serial.print("Found a data point, index:");
  Serial.println(index);
  Serial.print("At:");
  Serial.println(at);
  Serial.print("Value:");
  Serial.println(value);
}

void setup() {
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for Leonardo only
  }

  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("WiFi shield not present");
    while(true);
  }

  while ( status != WL_CONNECTED) {
    Serial.print("Attempting to connect to SSID: ");
    Serial.println(ssid);
    // Connect to WPA/WPA2 network. Change this line if using open or WEP network:
    status = WiFi.begin(ssid, pass);

    // wait 10 seconds for connection:
    delay(10000);
  }
  Serial.println("Connected to wifi");
  printWifiStatus();
}

void loop() {
  int response = m2xClient.receive(feedId, streamName, on_data_point_found, NULL);
  Serial.print("M2x client response code: ");
  Serial.println(response);

  if (response == -1) while(1) ;

  delay(5000);
}

void printWifiStatus() {
  // print the SSID of the network you're attached to:
  Serial.print("SSID: ");
  Serial.println(WiFi.SSID());

  // print your WiFi shield's IP address:
  IPAddress ip = WiFi.localIP();
  Serial.print("IP Address: ");
  Serial.println(ip);

  // print the received signal strength:
  long rssi = WiFi.RSSI();
  Serial.print("signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
}
