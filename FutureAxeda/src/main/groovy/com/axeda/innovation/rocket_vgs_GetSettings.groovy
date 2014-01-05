import com.axeda.drm.sdk.Context
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import com.axeda.drm.sdk.mobilelocation.CurrentMobileLocationFinder
import com.axeda.drm.sdk.mobilelocation.MobileLocation
import com.axeda.drm.sdk.mobilelocation.MobileLocationFinder
import com.axeda.sdk.v2.dsl.Bridges
import com.axeda.services.v2.HistoricalDataItemValueCriteria
import groovy.json.JsonSlurper
import net.sf.json.JSONObject
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import com.axeda.drm.sdk.scripto.Request

def contentType = "application/json"

/*
POST:
{
  "modelNumber": "aasdasfd",
  "serialNumber": "aasdfasf"
}

{
  "device": {
    "modelNumber": "aasdasfd",
    "serialNumber": "aasdfasf",
    "lastContact": "Wed Jan 12, 2013 12:12:12 PM",
    "platformStatus": "Connected",
    "serialStatus": "Connected",
    "protocolStatus": "Connected"
  },
  "application": {
    "appName": "This test application",
    "logoURL": "http://www.logologo.com/logos/generic-globe-vector-logo.jpg"
  },
  "layout": {
    "widgets": [
      {
        "type": "map",
        "width": "100",
        "title": "Map",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "line",
        "width": "50",
        "title": "Line",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "pie",
        "width": "50",
        "title": "Pie",
        "dataitemName": "sfgfdfreytegfd"
      },
      {
        "type": "bar",
        "width": "33",
        "title": "Bar",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "pie",
        "width": "33",
        "title": "Pie",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "image",
        "width": "33",
        "title": "Image",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "bar",
        "width": "25",
        "title": "Bar",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "line",
        "width": "25",
        "title": "Line",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "line",
        "width": "25",
        "title": "Line",
        "dataitemName": "sfgfgfd"
      },
      {
        "type": "map",
        "width": "25",
        "title": "Map",
        "dataitemName": "sfgfgfd"
      }
    ]
  }
}

 */

// Global try/catch. Gotta have it, you never know when your code will be exceptional!
try {

// build up the device info

// return the app name

// return the list of widgets
    StringBuilder outputString = new StringBuilder()

    // all of our Request Parameters are available here
    def parameters = Request.parameters
    // if you want to post content directly to this service, it can be accessed in 3 ways:
    // get the POSTed content as a String
    def body = Request.body
    // get an inputStream of the POSTed content (great for binary)
    def inputStream = Request.inputStream
    // or, if this is a multi-part form with attachments, get your attachments here
    def attachments = Request.attachments

    // Create a JSON Builder
    def jsonIn = new JsonSlurper().parseText(Request.body)
    def dataItems = []
    def modelNumber = jsonIn.modelNumber
    def serialNumber = jsonIn.serialNumber


    def widgetListName = serialNumber + "_widgets".replaceAll("_","-")
    def appListName = serialNumber + "_apps".replaceAll("_","-")

    // process the app info
    def appsList = Bridges.extendedMapBridge.find(appListName)
    // the first entry is the appname, the second is the URL
    def appName = appsList.map.size()>0 ? appsList.map.getAt(0).value : "Unknown"
    def imageUrl = appsList.map.size()==2 ? appsList.map.getAt(1).value : ""

    // Uusing the V1 SDK Pattern here. Everything centers around the "Context"
    final def CONTEXT = Context.getSDKContext()

    DataItem dataitem

    // This is the V1 "Finder" pattern. You will find this for most domain objects.
    ModelFinder modelFinder = new ModelFinder(CONTEXT)
    modelFinder.setName(modelNumber)
    Model model = modelFinder.find()

    // THe Finder acts as the Criteria for finding an object. You can use code completion to see the available criteria
    DeviceFinder deviceFinder = new DeviceFinder(CONTEXT)
    deviceFinder.setModel(model)
    deviceFinder.setSerialNumber(serialNumber)
    Device device = deviceFinder.find()

    outputString.append("{\n  \"device\" : {\n")
    outputString.append("    \"serialNumber\":\"${serialNumber}\",\n")
    outputString.append("    \"modelNumber\":\"${modelNumber}\",\n")
    outputString.append("    \"lastContact\":\"${device.lastContactDate.toString()}\",\n")
    def statusString = device.missing ? "Missing" : "Connected"
    outputString.append("    \"platformStatus\":\"${statusString}\",\n")
    outputString.append("    \"serialStatus\":\"Connected\",\n")
    outputString.append("    \"protocolStatus\":\"Connected\"\n")
    outputString.append("  },\n")
    outputString.append("\n  \"application\": {\n")
    outputString.append("    \"appName\":\"${appName}\",\n")
    outputString.append("    \"logoUrl\":\"${imageUrl}\"\n")
    outputString.append("  },\n")
    outputString.append("\n  \"layout\": {\n    \"widgets\": [\n")

    // look up the widgets
    def widgetsList = Bridges.extendedMapBridge.find(widgetListName)
    def widgets = widgetsList.map
    def first = true
    widgets?.each() { widget ->
        if (first) {
            outputString.append("      ${widget.value.replaceAll("@@","\"")}")
            first = false
        } else {
            outputString.append(",\n      ${widget.value.replaceAll("@@","\"")}")
        }
    }

    outputString.append("\n    ]\n  }\n}")

/*
      "layout": {
    "widgets": [
      {
        "type": "map",
        "width": "100",
        "title": "Map",
        "dataitemName": "sfgfgfd"
      },

     */

    // return the json map contents
    // we specify the content type, and any object as the return (even an outputstream!)
    return ["Content-Type": contentType,"Content":outputString.toString()]

    // alternately you may just want to serial an Object as JSON:
    // return ["Content-Type": contentType,"Content":JSONArray.fromObject(invertedMessages).toString(2)]

} catch (Exception e) {

    // I knew you were exceptional!
    // we'll capture the output of the stack trace and return it in JSON
    StringWriter sw = new StringWriter()
    PrintWriter ps = new PrintWriter(sw)
    e.printStackTrace(ps)

    def json = new JsonBuilder()
    // Build up our output
    json.Exception (
            description: "Execution Failed!!! An Exception was caught...",
            name: e.getMessage(),
            stack: sw.toString(),
    )

    // return the output
    return ["Content-Type":contentType,"Content":json.toPrettyString()]
}
