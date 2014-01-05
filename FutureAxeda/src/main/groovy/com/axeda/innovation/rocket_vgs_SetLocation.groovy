
import com.axeda.drm.sdk.Context
import com.axeda.drm.sdk.customobject.Call
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import com.axeda.drm.sdk.mobilelocation.MobileLocation
import groovy.json.JsonBuilder
import com.axeda.drm.sdk.scripto.Request

def contentType = "application/json"


/*
Called from a Rule
{
    serial: serialNumber
    location: "lat, long"
}

 */

// Global try/catch. Gotta have it, you never know when your code will be exceptional!
try {

    // all of our Request Parameters are available here
    String modelNumber = "KONTRON_M2MDev"
    def serialNumber = parameters.serial
    String location = parameters.location

    logger.info "Processing location: $location"

    // Uusing the V1 SDK Pattern here. Everything centers around the "Context"
    final def CONTEXT = Context.getSDKContext()

    // This is the V1 "Finder" pattern. You will find this for most domain objects.
    ModelFinder modelFinder = new ModelFinder(CONTEXT)
    modelFinder.setName(modelNumber)
    Model model = modelFinder.find()

    // The Finder acts as the Criteria for finding an object. You can use code completion to see the available criteria
    DeviceFinder deviceFinder = new DeviceFinder(CONTEXT)
    deviceFinder.setModel(model)
    deviceFinder.setSerialNumber(serialNumber)
    Device device = deviceFinder.find()

    // check the location for general format
    if (!location.contains(",")) {
        // we have an error
        return ["Content-Type": "application/json","Content":respondWithError("Location must be in the format \"lat,long\"",location)]
    }

    def chunks = location.split(",")

    if (chunks.size() != 2) {
        return ["Content-Type": "application/json","Content":respondWithError("Location must be in the format \"lat,long\"",location)]
    }

    // now make sure they are lat/long
    def lat = chunks[0]
    def lng = chunks[1]

    logger.info "Lat: $lat, Long: $lng"

    try {
        if ((Double.valueOf(lat) <= 90.0) && (Double.valueOf(lat) >= -90.0)) {
            // lat looks ok
            logger.info "Lat looks OK"
        } else {
            logger.info "Lat looks NOT OK"
            return ["Content-Type": "application/json","Content":respondWithError("Latitude must be a Float between -90.0 and 90.0",location)]
        }

        if ((Double.valueOf(lng) <= 180.0) && (Double.valueOf(lng) >= -180.0)) {
            // long looks ok
            logger.info "Lng looks OK"
        } else {
            logger.info "Lng looks NOT OK"
            return ["Content-Type": "application/json","Content":respondWithError("Longitude must be a Float between -180.0 and 180.0",location)]
        }

    } catch (Exception e) {
        e.printStackTrace()
    }

    // we are a go, set the location
    MobileLocation loc = new MobileLocation(CONTEXT,Double.valueOf(lat),Double.valueOf(lng),0,System.currentTimeMillis(),System.currentTimeMillis(),device.id.longValue)
    loc.store()

    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"modelNumber\": \"${modelNumber}\",\n")
    outputString.append("  \"serialNumber\": \"${serialNumber}\",\n")
    outputString.append("  \"location\": \"${location}\"\n")
    outputString.append("\n}")

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

def respondWithError(error,location) {
    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"error\": \"${error}\",\n")
    outputString.append("  \"location\": \"${location}\"\n")
    outputString.append("\n}")
    return outputString.toString()
}

def createAlarmEntry(Context CONTEXT, String alarmName, com.axeda.drm.sdk.device.Device device, int severity){
    AlarmEntry alarmEntry=new AlarmEntry(CONTEXT, device, alarmName, severity).store()
    alarmEntry
}


