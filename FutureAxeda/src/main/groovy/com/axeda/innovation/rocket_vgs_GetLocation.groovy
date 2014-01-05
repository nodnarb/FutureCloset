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
  "serialNumber": "aasdfasf"
}

Which returns
{
  "modelNumber": "aasdasfd",
  "serialNumber": "aasdfasf",
 "location": "-1.0,1.0"
}
 */

// Global try/catch. Gotta have it, you never know when your code will be exceptional!
try {

    def defaultLocation = "47.613917,-122.194796"
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
    def outerMap = [:]
    def dataValues = []
    def modelNumber = jsonIn.modelNumber
    def serialNumber = jsonIn.serialNumber
    modelNumber = "KONTRON_M2MDev"

    outerMap.put("modelNumber","${modelNumber}")
    outerMap.put("serialNumber","${serialNumber}")

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


    CurrentMobileLocationFinder finder = new CurrentMobileLocationFinder(CONTEXT);
    finder.setDeviceId(device.id.longValue);
    MobileLocation mobileLocation = finder.find();
    def locationString = defaultLocation
    if (mobileLocation?.lat != null) {
        locationString = mobileLocation.lat +","+ mobileLocation.lng
    }

    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"modelNumber\": \"${modelNumber}\",\n")
    outputString.append("  \"serialNumber\": \"${serialNumber}\",\n")
    outputString.append("  \"location\": \"${locationString}\"\n}")

    // return the json map contents
    // we specify the content type, and any object as the return (even an outputstream!)
    return ["Content-Type": contentType,"Content":outputString]

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


