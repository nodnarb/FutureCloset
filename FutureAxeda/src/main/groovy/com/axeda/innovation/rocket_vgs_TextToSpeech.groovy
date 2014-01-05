import com.axeda.sdk.v2.bridge.FileInfoBridge
import com.axeda.services.v2.Asset
import com.axeda.services.v2.ExecutionResult
import com.axeda.services.v2.FileInfo
import com.axeda.services.v2.FileUploadSession
import groovyx.net.http.HTTPBuilder
import net.sf.json.JSONArray
import net.sf.json.groovy.JsonSlurper
import org.apache.commons.httpclient.Header
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient

import static com.axeda.sdk.v2.dsl.Bridges.getFileInfoBridge
import static com.axeda.sdk.v2.dsl.Bridges.getFileUploadSessionBridge

import com.axeda.sdk.v2.dsl.Bridges
import com.axeda.drm.sdk.scripto.Request

/**
 * vgs_TextToSpeech
 *
 * Expected input:
 *
 * GET
 *
 * ?message=Hello%20World&serialNumber(the ID of the device to associate with this)
 */

def message = Request.parameters.message
def serialNumber = Request.parameters.serialNumber
def model = "KONTRON_M2MDev"

try {

    // check to see if there is a leading "1"
    if ((!serialNumber) || (!message)){
        return ["Content-Type":"application/json","Content":respondWithError("Error : Need to supply both a serialNumber as a source, and a message to convert to text",message)]
    }


    // we received the serialNumber, which will be the serial #
    def assetName = model+"||"+serialNumber
    Asset asset = Bridges.assetBridge.find(assetName)
    if (asset == null || (asset?.systemId == -1)) {
        return ["Content-Type":"application/json","Content":respondWithError("Error : Asset cannot be found for given serialNumber. Are you sure the serial number of your device is: ${serialNumber}?",message)]
    }

    def oauthResponse = Bridges.customObjectBridge.execute("vgs_GetOauthToken",[:])
    logger.info "Found OAuth response: ${oauthResponse.Content}"
    def slurper = new JsonSlurper().parseText(oauthResponse.Content as String)

    def accessToken = slurper.access_token
    def refreshToken = slurper.refresh_token
    def error = slurper.error as String

    if (error) {
        // we have an error
        logger.info "Unable to get Oauth Access Token ${error}.\n"
        return ["Content-Type":"application/json","Content":respondWithError("Error : Unable to get Oauth Access Token ${error}",message)]
    }

    logger.info "Access Token found: $accessToken"

//    def http = new HTTPBuilder()
// Create an instance of HttpClient.
    DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpPost httpost = new HttpPost("https://api.att.com/speech/v3/textToSpeech");
    httpost.addHeader("Accept", "audio/x-wav")
    httpost.addHeader("Content-Type","text/plain")
    httpost.addHeader("Authorization","Bearer ${accessToken}")

    StringEntity entity = new StringEntity(message)
    httpost.setEntity(entity)
    response = httpclient.execute(httpost);
    def responseEntity = response.getEntity()
    def inputStream = responseEntity.content

    def outerMap = [:]


//    http.post(uri: "https://api.att.com/speech/v3/textToSpeech", contentType: ContentType.TEXT, body: message, headers: ["Accept": "audio/x-wav", "Content-Type":"text/plain", "Authorization":"Bearer ${accessToken}"]) {
//        resp, java.io.InputStreamReader inputStream ->
//            // convert the inputstream
            def out = new ByteArrayOutputStream()
            IOUtils.copy(inputStream,out)

            FileInfoBridge fib = fileInfoBridge
            FileInfo myAudioFile = new FileInfo(filelabel: "TextToSpeech"+System.currentTimeMillis(),
                    filename: "TextToSpeech"+System.currentTimeMillis()+".wav",
                    filesize: out?.size(),
                    description: "Created via the TextToSpeech API from AT&T",
                    tags: "${serialNumber},TYPE:AUDIO,TTS"
            )

            myAudioFile.contentType = "audio/wav"

            FileUploadSession fus = new FileUploadSession();
            fus.files = [myAudioFile]

            ExecutionResult fer = fileUploadSessionBridge.create(fus);
            myAudioFile.sessionId = fer.succeeded.getAt(0)?.id

            ExecutionResult fileInfoResult = fib.create(myAudioFile)

            if (fileInfoResult.successful) {
                outerMap.fileInfoSave = "File Info Saved"
                outerMap.sessionId = "File Upload SessionID: "+fer.succeeded.getAt(0)?.id
                outerMap.fileInfoId = "FileInfo ID: "+fileInfoResult?.succeeded.getAt(0)?.id
                ExecutionResult er = fib.saveOrUpdate(fileInfoResult.succeeded.getAt(0).id,new ByteArrayInputStream(out.toByteArray()))
                def fileInfoId = fileInfoResult?.succeeded.getAt(0)?.id
                String url = "/services/v1/rest/Scripto/execute/vgs_DownloadFile?fileId=${fileInfoId}"
                if (er.successful) {
                    outerMap.url = url
                } else {
                    outerMap.save = "false"
                    logger.info(logFailure(er,outerMap))
                }
            } else {
                logger.info(logFailure(fileInfoResult, outerMap))
            }

            // we have to check for <faultstring> even if response is 200
            return ["Content-Type":"application/json","Content":JSONArray.fromObject(outerMap).toString(2)]
//    }
} catch(e) {
    def stack = ExceptionUtils.getFullStackTrace(e)
    logger.info "FAILED: Exception occurred: ${e.message}, ${stack}"
    return ["Content-Type":"application/json","Content":respondWithError("Error : FAILED: Exception occurred: ${e.message}, ${stack}",message)]
}

private void logFailure(ExecutionResult fileInfoResult, LinkedHashMap outerMap) {
    outerMap.message = fileInfoResult.failures.getAt(0)?.message
    outerMap.source = fileInfoResult.failures.getAt(0)?.sourceOfFailure
    outerMap.details = fileInfoResult.failures.getAt(0)?.details?.toString()
    outerMap.fileInfoSave = "false"
}

def respondWithError(error,message) {
    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"error\": \"${error}\",\n")
    outputString.append("  \"message\": \"${message}\"\n")
    outputString.append("\n}")
    return outputString.toString()
}


