mport com.axeda.drm.sdk.Context
import com.axeda.drm.sdk.agent.commands.CommandStatus
import com.axeda.drm.sdk.agent.commands.SetDataItem
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
  "serialNumber": "aasdfasf",
  "message": "this is my message"
}

Which returns me
{
  "modelNumber": "aasdasfd",
  "serialNumber": "aasdfasf",
  "status": "Waiting to Deliver",
  "statusId": "73"
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
    def message = jsonIn.message

    modelNumber = "KONTRON_M2MDev"

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

    // send the command
    def (response,id)= sendCommand(device,model,message)

    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"modelNumber\": \"${modelNumber}\",\n")
    outputString.append("  \"serialNumber\": \"${serialNumber}\",\n")
    // status
    outputString.append("  \"status\": \"${response}\",\n")
    outputString.append("  \"statusId\": \"${id}\"\n")
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


def sendCommand(Device device, Model model, String dataItemValue) {
    Context ctx = Context.create()
    def returnList = []
    try {
        String dataItemName = "DownstreamCMD"
//        String dataItemName = "test"
        DataItemFinder dif = new DataItemFinder(ctx)
        dif.setDataItemName(dataItemName)
        dif.setModel(model)
        DataItem di = dif.find()
        if (di == null) {
            di = new DataItem(ctx,model,com.axeda.drm.services.device.DataItemType.STRING,dataItemName)
            di.store()
            dif.setDataItemName(dataItemName)
            dif.setModel(model)
            di = dif.find()
        }

        if (di == null) {
            throw new Exception("Unable to create DataItem")
        } else if (device == null) {
            throw new Exception("Unable to find Device")
        } else {
            SetDataItem sdi = new SetDataItem(ctx,di,dataItemValue)
            CommandStatus cs = sdi.send(device)
            return [cs.status.name,cs.id]
        }

    } catch (NullPointerException npe) {
        throw new Exception("Null Pointer: ${npe.message}",npe);
    } catch (Exception e) {
        throw new Exception("Exception: ${e.message}");
    }
}


