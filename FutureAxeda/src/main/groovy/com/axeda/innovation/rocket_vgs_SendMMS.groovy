import com.axeda.services.v2.FileInfo
import groovyx.net.http.HTTPBuilder
import net.sf.json.groovy.JsonSlurper
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource
import org.apache.commons.httpclient.methods.multipart.Part
import org.apache.commons.httpclient.methods.multipart.StringPart
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.commons.httpclient.methods.multipart.FilePart
import org.apache.commons.io.IOUtils
import com.axeda.sdk.v2.dsl.Bridges
import com.axeda.drm.sdk.scripto.Request
import org.apache.http.message.BasicHeader
import org.apache.http.params.HttpParams
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.Method.POST

import java.io.*;
import org.apache.log4j.Logger;

// httpclient-3.1 imports
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;


/**
 * vgs_SendSMS
 *
 * Expected input:
 *
 * GET
 *
 * ?message=Hello%20World&target=5085071456
 */

String message = Request.parameters.message
String target = Request.parameters.target
def fileId = Request.parameters.fileId

try {

    // check to see if there is a leading "1"
    if ((!target || !message) || (target?.length()<10) || (target?.length()>11)){
        def errorMessage = """{
        "Status":"Need to supply both a target phone number in the form: 1XXXXXXX, and a message to send"
    }
"""
        return ["Content-Type":"application/json","Content":errorMessage]
    }

    if (target?.length()==10) {
        target = "1" + target
    }

    def oauthResponse = Bridges.customObjectBridge.execute("vgs_GetOauthToken",[:])
    logger.info "Found OAuth response: ${oauthResponse.Content}"
    def slurper = new JsonSlurper().parseText(oauthResponse.Content as String)

    /*
        {
        "access_token":"UDQgj6dKGFrrOGHrO09yWhIS3Gv6koyR",
        "refresh_token":"OEomlL8RQpi8QlxPI0xppWaCDVzgO9yL",
        "error":"usually empty"
    }
     */

    def accessToken = slurper.access_token
    def refreshToken = slurper.refresh_token
    def error = slurper.error as String

    if (error) {
        // we have an error
        logger.info "Unable to get Oauth Access Token ${error}.\n"
        return ["Content-Type":"application/json","Content":"Unable to get Oauth Access Token ${error}.\n"]
    }


    logger.info "Access Token found: $accessToken"

    def bodyString = """{
    "outboundSMSRequest": {
        "address": "tel:+${target}",
        "message":"${message}"
    }
}
"""

    // build up the body
    // file ID 14331
    InputStream fis = Bridges.fileInfoBridge.getFileData(fileId)
    def filename = ""
    File tempFile = null
    FileOutputStream fos = null
    logger.info "Writing file"
    try {
        tempFile = File.createTempFile(System.currentTimeMillis()+"MMS",".tmp")
        filename = tempFile.name
        logger.info "File written, name: $filename"
        fos = new FileOutputStream(tempFile)
        IOUtils.copy(fis,fos)
        logger.info "streams copied"
    } finally {
        try {
            fos.close()
        } catch (Exception e){}
    }

    def responseString = ""
//    def targetURL = "https://api.att.com/3/smsmessaging/outbound/44628912/requests"
//    def targetURL = "https://api.att.com/mms/v3/messaging/outbox"
    def targetURL = "http://platform.axeda.com/services/v1/rest/Scripto/execute/vgs_Debug?username=kholbrook&password=WhiPPing"
    boolean posted = postFile(targetURL,tempFile, target, message, accessToken)
    if (posted) {
        responseString = "{\"Status\":\"MMS Sent\"\n}"
    } else {
        responseString = "{\"Status\":\"MMS NOT Sent\"\n}"
    }

    // we have to check for <faultstring> even if response is 200
    return ["Content-Type":"application/json","Content":responseString]
} catch(e) {
    def stack = ExceptionUtils.getFullStackTrace(e)
    def errorMessage = """        {
        "access_token":"",
        "refresh_token":"",
        "error":"${e.message}, ${stack}"
    }
"""
    logger.info "FAILED: Exception occurred: ${e.message}, ${stack}"
    return ["Content-Type":"application/json","Content":errorMessage]
}

public boolean postFile(String targetURL,File targetFile, String msisdn, String message, String accessToken) {
    try {

//        def http = new HTTPBuilder("http://platform.axeda.com")
        def http = new HTTPBuilder("https://api.att.com")

        def returnVal = http.request(POST) { request ->
            uri.path = "/3/smsmessaging/outbound/44628912/requests"
            uri.query = ["address":"tel:$msisdn","subject":message,"priority":"High"]
            requestContentType = 'multipart/form-data'
            headers = ["Accept":"application/json", "Authentication":"Bearer $accessToken"]
            def params = ["address":"tel:$msisdn","subject":message,"priority":"High"]
            Part[] parts = new Part[1];
            parts[0] = new FilePart(targetFile.getName(), targetFile)

            logger.info "Parts: ${parts.class.name}, Params: ${request.params.class.name}"
            request.entity = new MultipartEntityWrapper(parts, params)

            response.success = {resp, xml ->
                if (resp.data?.faultstring?.size() > 0) {
                    logger.info "FAILED with \n" + outputFormattedXml(xml)
                    return false
                }
                else {
                    logger.info "\tSUCCEEDED"
                    return true
                }
            }
            response.failure = { resp ->
                logger.info "FAILED with HTTP $resp.status:$resp.statusLine"
                return false
            }
            return false
        }

//        HttpClient client = new DefaultHttpClient();
//
//        def post = new HttpPost(targetURL);
//        def params = ["address":"tel:$msisdn","subject":message,"priority":"High"]
//
//        logger.info("Uploading " + targetFile.getName() + " to " + targetURL);
//        Part[] parts = new Part[2]
//        parts[0] = new FilePart(targetFile.getName(), targetFile)
//        MultipartEntityWrapper mre = new MultipartEntityWrapper(parts,params)
//        post.setEntity(mre)
//        post.addHeader("Accept","application/json")
//        post.addHeader("Authorization","Bearer ${accessToken}")
//        post.params.setParameter("address","tel:$msisdn")
//
//        def response = client.execute(post)
//
//        if (response.getStatusLine().statusCode == 200) {
//            logger.info(
//                    "Upload complete, response=200, ${response.getStatusLine().reasonPhrase}"
//            );
//            return true
//        } else {
//            logger.info(
//                    "Upload failed, response=${response.getStatusLine().statusCode}, ${response.getStatusLine().reasonPhrase}"
//            );
//            return false
//        }
    } catch (Exception ex) {
        logger.info("Error: " + ex.getMessage());
        ex.printStackTrace();
    } finally {

    }
}

 class MultipartEntityWrapper extends BasicHttpEntity {

    protected MultipartRequestEntity multipartLegacy;

    /**
     * Constructs a new MultipartRequestEntity compatible with HttpClient-4
     *
     * @param parts The parts to include in the entity
     * @param params - WARNING, mapping of new parameters to old parameters is -NOT- done
     */
    public MultipartEntityWrapper(Part[] parts, Map params) {
        HttpMethodParams targetParams = new HttpMethodParams();
        params.each() { String k, String v ->
            targetParams.setParameter(k,v)
        }

        multipartLegacy = new MultipartRequestEntity(parts, targetParams);
    }

    /**
     * @see org.apache.http.HttpEntity#getContentLength()
     */

    public long getContentLength() {
        return multipartLegacy.getContentLength();
    }

    /**
     * @see org.apache.http.HttpEntity#getContentType()
     */
    public Header getContentType() {
        // TODO: Find the constant for Content-Type? :)
        return new BasicHeader("Content-Type", multipartLegacy.getContentType());
    }

    /**
     * @see org.apache.http.HttpEntity#isChunked()
     */
    public boolean isChunked() {
        // Roland Webers word is law;
        // (http://www.nabble.com/multipart-form-data-in-4.0-td14224819.html)
        return getContentLength()<0L;
    }

    /**
     * @see org.apache.http.HttpEntity#isRepeatable()
     */
    public boolean isRepeatable() {
        return multipartLegacy.isRepeatable();
    }

    /**
     * @see org.apache.http.HttpEntity#writeTo(java.io.OutputStream)
     */
    public void writeTo(OutputStream outstream) throws IOException {
        multipartLegacy.writeRequest(outstream);
    }

    /**
     *  Receiving not supported.
     */
    public void consumeContent() throws IOException {
        throw new UnsupportedOperationException("MultipartEntityWrapper has no support for receiving");
    }

    /**
     *  Receiving not supported.
     */
    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("MultipartEntityWrapper has no support for receiving");
    }

    /**
     *  Receiving not supported.
     */
    public Header getContentEncoding() {
        return null;
    }

    /**
     *  Receiving not supported.
     */
    public boolean isStreaming() {
        return false;
    }
}
