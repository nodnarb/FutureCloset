import com.axeda.drm.sdk.Context
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import com.axeda.drm.sdk.mobilelocation.CurrentMobileLocationFinder
import com.axeda.drm.sdk.mobilelocation.MobileLocation
import com.axeda.drm.sdk.mobilelocation.MobileLocationFinder
import com.axeda.services.v2.HistoricalDataItemValueCriteria
import groovy.json.JsonSlurper
import net.sf.json.JSONObject
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import com.axeda.drm.sdk.scripto.Request
 
def contentType = "application/json"
 
 
/*
POST
{
  "modelNumber": "aasdasfd",
  "serialNumber": "aasdfasf",
 "dataitemName": "sfgfgfd",
 "verbose": "true",
 "numValues": "25"
}
 
Which returns me
{
  "modelNumber": "aasdasfd",
  "serialNumber": "aasdfasf",
 "dataitemName": "sfgfgfd",
 "numValues": "25",
"dataitemValues":[65,59,90,81,56,55,40]
}
 */
 
// Global try/catch. Gotta have it, you never know when your code will be exceptional!
try {
 
    // all of our Request Parameters are available here
    def parameters = Request.parameters
    // if you want to post content directly to this service, it can be accessed in 3 ways:
    // get the POSTed content as a String
    def body = Request.body

// Create a JSON Builder
    def jsonIn = new JsonSlurper().parseText(body)
    def outerMap = [:]
    def dataValues = []
    def modelNumber = jsonIn.modelNumber
    def serialNumber = jsonIn.serialNumber
    def dataItemName = jsonIn.dataitemName
    def numValues = jsonIn.numValues
    def verbose = jsonIn.verbose?.toBoolean() // if set to true, will return timestamps
    modelNumber = "KONTRON_M2MDev"

    if (!numValues) {
        numValues = 25
    } else {
        numValues = numValues as int
    }
    if (numValues > 100) {
        numValues = 100
    } else if (numValues < 1) {
        numValues = 1
    }

    outerMap.put("modelNumber","${modelNumber}")
    outerMap.put("serialNumber","${serialNumber}")
    outerMap.put("dataItemName","${dataItemName}")
    outerMap.put("numValues","${numValues}")

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
 
    // Data values can be current (a single value per data item) or historical (N values).
    // Use the HistoricalDataFinder to lookup values in the past
    HistoricalDataFinder hdFinder = new HistoricalDataFinder(CONTEXT, device)
    hdFinder.setLastNValues(numValues)
    DataItemFinder dif = new DataItemFinder(CONTEXT)
    dif.setDataItemName(dataItemName)
    dif.setModel(model)
    dif.setModel(model)
    DataItem dataItem = dif.find()
 
//
//  CurrentDataFinder cdFinder = new CurrentDataFinder(CONTEXT, device)
//    DataValueList dvalues = cdFinder.find()
    HistoricalDataItemValueCriteria crit = new HistoricalDataItemValueCriteria()
    crit.assetId = device.id
    crit.pageSize = numValues
    crit.startDate = new Date(1357179934000)
    def dvalues = hdFinder.find(dataItem)
 
    outerMap.put("dataItemValues",dvalues?.list)
 
//    CurrentMobileLocationFinder finder = new CurrentMobileLocationFinder(CONTEXT);
//    finder.setDeviceId(device.id.longValue);
//    MobileLocation mobileLocation = finder.find();
    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"modelNumber\": \"${modelNumber}\",\n")
    outputString.append("  \"serialNumber\": \"${serialNumber}\",\n")
    outputString.append("  \"dataItemName\": \"${dataItemName}\",\n")
    outputString.append("  \"dataItemValues\": [")
    def first = true
    dvalues?.list?.each() { val ->
        if (first) {
            first = false
        } else {
            outputString.append(",")
        }
		if (verbose){
            def friendlytimestamp = val.timestamp.format("HH:mm:ss a")
			outputString.append("{\"value\":\"${val.asString()}\",\"timestamp\":\"${friendlytimestamp}\"}\n")
		}
		else {
			outputString.append("\"${val.asString()}\"")
		}
    }
    outputString.append("]\n}")
 
    // return the json map contents
    // we specify the content type, and any object as the return (even an outputstream!)
    return ["Content-Type": contentType,"Content":outputString]
 
    // alternately you may just want to serial an Object as JSON:
    // return ["Content-Type": contentType,"Content":JSONArray.fromObject(invertedMessages).toString(2)]
 
} catch (Exception e) {
 
    // I knew you were exceptional!
    // we\"ll capture the output of the stack trace and return it in JSON
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
