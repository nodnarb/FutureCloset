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
 
/**
* vgs_GetDeviceStatus.groovy
* -----------------------
*
* Get the connected status of a device
*
* @params
*  modelNumber (REQUIRED) Str - the model number of an asset
*  serialNumber (REQUIRED) Str - the serial number of an asset
*
*
* @author sara streeter <sstreeter@axeda.com>
 *
*/
 
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
    def jsonIn
    def modelNumber 
    def serialNumber 
    def dataItems
    if (body){
    jsonIn = new JsonSlurper().parseText(body)
    dataItems = []
    modelNumber = jsonIn.modelNumber
    serialNumber = jsonIn.serialNumber
    }
    else if (parameters.modelNumber && parameters.serialNumber){
        modelNumber = parameters.modelNumber
        serialNumber = parameters.serialNumber
    }
    // Using the V1 SDK Pattern here. Everything centers around the "Context"
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
    
    HistoricalAlarmFinder halarmFinder = new HistoricalAlarmFinder(CONTEXT);  
      halarmFinder.setDevice(device)
      halarmFinder.setAlarmName("SerialStatus")
      halarmFinder.setState(AlarmState.STARTED)
      
      
      def histserialalarms = halarmFinder.findAll()
      
      halarmFinder.setAlarmName("ProtocolStatus")
    halarmFinder.setState(AlarmState.STARTED)
    def histprotocolalarms = halarmFinder.findAll()
	
	AlarmFinder alarmFinder = new AlarmFinder(CONTEXT)
    alarmFinder.setDevice(device)
    alarmFinder.setAlarmName("SerialStatus")
	alarmFinder.setState(AlarmState.STARTED)
    List serialalarms = alarmFinder.findAll()
    serialalarms += histserialalarms
	
    alarmFinder.setAlarmName("ProtocolStatus")
	alarmFinder.setState(AlarmState.STARTED)
    List protocolalarms = alarmFinder.findAll()
    
    protocolalarms += histprotocolalarms
 
        if (device == null){
        outputString.append("{\n  \"device\" : {\n")
        outputString.append("    \"serialNumber\":\"${serialNumber}\",\n")
        outputString.append("    \"modelNumber\":\"${modelNumber}\",\n")
        outputString.append("    \"status\":\"NOT FOUND\",\n")
        outputString.append("  }\n \n   \n}")
    }
    else {
        outputString.append("{\n  \"device\" : {\n")
        outputString.append("    \"serialNumber\":\"${serialNumber}\",\n")
        outputString.append("    \"modelNumber\":\"${modelNumber}\",\n")
        outputString.append("    \"lastContact\":\"${device?.lastContactDate?.toString()}\",\n")
        def statusString = device?.missing ? "Missing" : "Connected"
        outputString.append("    \"platformStatus\":\"${statusString}\",\n")
		def serialString = serialalarms?.size() > 0 ? "Missing" : "Connected"
        outputString.append("    \"serialStatus\":\"${serialString}\",\n")
		def protocolString = protocolalarms?.size() > 0 ? "Missing" : "Connected"
        outputString.append("    \"protocolStatus\":\"${protocolString}\"\n")
        outputString.append("  }\n \n   \n}")
    }
 
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
