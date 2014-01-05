import groovyx.net.http.HTTPBuilder
import net.sf.json.groovy.JsonSlurper
import org.apache.commons.lang.exception.ExceptionUtils

import static groovyx.net.http.Method.PUT
import static groovyx.net.http.ContentType.JSON
import com.axeda.sdk.v2.dsl.Bridges
import com.axeda.drm.sdk.scripto.Request

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
def target = Request.parameters.target


try {

    // limit the message to 4096
    if (message.length()>4096) {
        message = message.substring(0,4096)
    }

    def http = new HTTPBuilder("http://api-m2x.att.com/v1/")

    //def bodyString = message
    def bodyString = """{
        "value":"${message}"
}"""

    logger.info "Body $bodyString"
    http.request(PUT, JSON) {
        headers = ["X-M2X-KEY":"1a08ccc1f387096e8774946cc88a24e9", "Content-Type":"application/json"]
        uri.path = 'feeds/aa339e4f6f75c439e40274b986071d80/streams/clothes'
        requestContentType =  JSON

        body = bodyString
        response.success = {resp ->
            def responseString = """
{
    "Status":"Sent",
    "value":"${message}"
}
"""

            return ["Content-Type":"application/json","Content":responseString]
        }
        // response code is not 200 OK
        response.failure = {resp ->
            logger.info "FAILED with HTTP $resp.status:$resp.statusLine\n"
            error = """{
        "Message":"${message}",
    "Status":"Error: $resp.status:$resp.statusLine."
}"""
            return ["Content-Type":"application/json","Content":error]
        }
    }
} catch(e) {
    def stack = ExceptionUtils.getFullStackTrace(e)
    def errorMessage = """        {
        "value":"${message}",
        "Status":"${e.message}, ${stack}"
    }
"""
    logger.info "FAILED: Exception occurred: ${e.message}, ${stack}"
    return ["Content-Type":"application/json","Content":errorMessage]
}
