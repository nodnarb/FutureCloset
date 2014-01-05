import com.axeda.common.sdk.id.Identifier
import com.axeda.drm.sdk.Context
import com.axeda.drm.sdk.agent.commands.CommandStatus
import com.axeda.drm.sdk.agent.commands.CommandStatusFinder
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import com.axeda.drm.sdk.mobilelocation.CurrentMobileLocationFinder
import com.axeda.drm.sdk.mobilelocation.MobileLocation
import com.axeda.drm.sdk.mobilelocation.MobileLocationFinder
import com.axeda.sdk.v2.dsl.Bridges
import com.axeda.services.v2.HistoricalDataItemValueCriteria
import groovy.json.JsonSlurper
import net.sf.json.JSONObject
import net.sf.json.JSONObject
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import com.axeda.drm.sdk.scripto.Request

def contentType = "application/json"


/*
     This is called directly from an ExpressionRule, passing in the serial # and the command value

     The legal values are:

    SMS|5085071456|Hello World! (sends the text Hello World! to the supplied phone number)
    TTS|00B3386EDA21|Hello World! Please make this into a sound file. (will replace the existing audio file by creating a new one)

 */

// Global try/catch. Gotta have it, you never know when your code will be exceptional!
try {

    StringBuilder outputString = new StringBuilder()

    // all of our Request Parameters are available here
    // if you want to post content directly to this service, it can be accessed in 3 ways:
    String serial = parameters.serial
    String command = parameters.command
    def legalCommands = ["HAM","MMSI","MMSA","TTS"]
    String modelNumber = "KONTRON_M2MDev"

    logger.info "Found command: $command"

    if ((command == null) || (command == "") || (!command?.contains("|"))) {
        return ["Content-Type": contentType,"Content":respondWithError("Commands must be in the form CMD|ARG1|ARG2|...",command)]
    }

    Context CONTEXT = Context.getSDKContext()
    // look up the device
    ModelFinder mf = new ModelFinder(CONTEXT)
    mf.setName(modelNumber)
    DeviceFinder df = new DeviceFinder(CONTEXT)
    df.setSerialNumber(serial)
    df.model = mf.find()
    Device d = df.find()
    if (d == null) {
        return ["Content-Type": contentType,"Content":respondWithError("Could not find Device with serial $serial, are you sure $serial is your SerialNumber?",command)]
    }

    def args = command?.tokenize("|")
    def cmd = args[0]

    if (!legalCommands.contains(cmd)) {
        return ["Content-Type": contentType,"Content":respondWithError("Legal Commands are: SMS,MMSI,MMSA,TTS",command)]
    }

    def result = "UNKNOWN"
    def success = false
    // we have a valid command arg, so process
    outputString.append("{\n")
    logger.info "Found something"
    switch (cmd) {
        case "HAM":
            logger.info "Found HAM"

            if (args.size()==2) {
                // we're good, otherwise error out
                def params = ["message":args[1]]
                def response = (Map)Bridges.customObjectBridge.execute("rocket_vgs_SendSMS",params)
                // response has 2 elements, Content and Content-Type
                def jsonString = response.Content
                def json = new JsonSlurper().parseText(jsonString)
                if (json.Status == "HAM Sent") {
                    // we're OK
                    result = "HAM Sent"
                    outputString.append("  \"status\": \"${result}\",\n")
                } else {
                    // we have failed, get the info from it
                    result = "HAM Send Failed: ${json?.Status}, ${json?.Message}"
                    outputString.append("  \"error\": \"${result}\",\n")
                }
            } else {
                result = "Expecting 2 args for HAM: HAM|Message"
                outputString.append("  \"error\": \"${result}\",\n")
            }
            break;
        case "MMSI":
            break;
        case "MMSA":
            break;
        case "TTS":
            logger.info "Found TTS"
            if (args.size()==3) {
                // we're good, otherwise error out
                def params = ["serialNumber":args[1],"message":args[2]]
                def response = (Map)Bridges.customObjectBridge.execute("vgs_TextToSpeech",params)
                // response has 2 elements, Content and Content-Type
                def jsonString = response.Content
                def json = new JsonSlurper().parseText(jsonString)
                String error = json.error
                def url = json.url
                logger.info "Got JSON Response: $jsonString"
                logger.info "Error: $error, URL: $url."
                if (error != null && ((!error.isEmpty()) && (!error?.equals("[null]")))) {
                    // we've got an error
                    result = "Text to Speech Conversion Failed!. Message: ${json.message}, ${json.error} "
                    outputString.append("  \"error\": \"${result}\",\n")
                    // create an alarm
                    createAlarmEntry(CONTEXT,"Text to Speech Conversion Failed!.",d,100)
                } else {
                    outputString.append("  \"status\": \"Text to Speech conversion successful\",\n")
                    outputString.append("  \"url\": \"${(json?.url as String)?.replaceAll("\\[","")?.replaceAll("\\]","")}\",\n")
                }
            } else {
                result = "Expecting 3 args for TextToSpeech: TTS|SerialNum|Message"
                outputString.append("  \"error\": \"${result}\",\n")
            }

            break;
        default:
            // not likely, since we pre-vetted the cmd above
            break;
    }

    outputString.append("  \"command\": \"${command}\"\n")
    outputString.append("\n}")

    // return the json map contents
    return ["Content-Type": contentType,"Content":outputString]

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

def respondWithError(error,command) {
    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"error\": \"${error}\",\n")
    outputString.append("  \"command\": \"${command}\"\n")
    outputString.append("\n}")
    return outputString.toString()
}


def createAlarmEntry(Context CONTEXT, String alarmName, com.axeda.drm.sdk.device.Device device, int severity){
    AlarmEntry alarmEntry=new AlarmEntry(CONTEXT, device, alarmName, severity).store()
    alarmEntry
}
