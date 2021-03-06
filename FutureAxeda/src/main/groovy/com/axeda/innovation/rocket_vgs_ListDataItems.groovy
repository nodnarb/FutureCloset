import com.axeda.drm.sdk.Context
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import com.axeda.drm.sdk.mobilelocation.CurrentMobileLocationFinder
import com.axeda.drm.sdk.mobilelocation.MobileLocation
import com.axeda.drm.sdk.mobilelocation.MobileLocationFinder
import com.axeda.services.v2.HistoricalDataItemValueCriteria
import groovy.json.JsonSlurper
import net.sf.json.JSONObject
import net.sf.json.JSONObject
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import com.axeda.drm.sdk.scripto.Request

def contentType = "application/json"


/*
POST
{
  "modelNumber": "aasdasfd",
  "serialNumber": "aasdfasf"
}

Which returns me
{
  "modelNumber": "aasdasfd",
  "serialNumber": "aasdfasf",
"dataItemNames":["DataItem1","DataItem2","DataItem3"]
}
 */

// Global try/catch. Gotta have it, you never know when your code will be exceptional!
try {

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

    // Uusing the V1 SDK Pattern here. Everything centers around the "Context"
    final def CONTEXT = Context.getSDKContext()

    // This is the V1 "Finder" pattern. You will find this for most domain objects.
    ModelFinder modelFinder = new ModelFinder(CONTEXT)
    modelFinder.setName(modelNumber)
    Model model = modelFinder.find()

    // THe Finder acts as the Criteria for finding an object. You can use code completion to see the available criteria
    DeviceFinder deviceFinder = new DeviceFinder(CONTEXT)
    deviceFinder.setModel(model)
    deviceFinder.setSerialNumber(serialNumber)
    Device device = deviceFinder.find()

    model.dataItems.each() { DataItem item ->
        dataItems.add(item.name)        
    }

    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"modelNumber\": \"${modelNumber}\",\n")
    outputString.append("  \"serialNumber\": \"${serialNumber}\",\n")
    outputString.append("  \"dataItemNames\": [")
    def first = true
    dataItems.each() { val ->
        if (first) {
            first = false
        } else {
            outputString.append(",")
        }
        outputString.append("\"${val}\"")
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


