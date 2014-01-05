import groovyx.net.http.HTTPBuilder
import net.sf.json.groovy.JsonSlurper
import org.apache.commons.lang.exception.ExceptionUtils

import static groovyx.net.http.Method.POST
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

    // check to see if there is a leading "1"
    if ((!target || !message) || (target?.length()<10) || (target?.length()>11)){
        def errorMessage = """{
        "Status":"Need to supply both a target phone number in the form: 1XXXXXXX, and a message to send"
    }
"""
        return ["Content-Type":"application/json","Content":errorMessage]
    }

    // prepend the "1" if it is not present
    if (target?.length()==10) {
        target = "1" + target
    }

    // limit the message to 4096
    if (message.length()>4096) {
        message = message.substring(0,4096)
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

    def http = new HTTPBuilder("https://api.att.com/3/")

    logger.info "Access Token found: $accessToken"

    def bodyString = """{
    "outboundSMSRequest": {
        "address": "tel:+${target}",
        "message":"${message}"
    }
}
"""

    logger.info "Body $bodyString"
    http.request(POST, JSON) {
        headers = ["Accept": "application/json", "Content-Type":"application/json", "Authorization":"Bearer ${accessToken}"]
        uri.path = 'smsmessaging/outbound/44628930/requests'
        requestContentType =  JSON

        body = bodyString
        response.success = {resp ->
            def responseString = """
{
    "Status":"SMS Sent",
    "Message":"${message}",
    "Target":"${target}"
}
"""

            return ["Content-Type":"application/json","Content":responseString]
        }
        // response code is not 200 OK
        response.failure = {resp ->
            logger.info "FAILED with HTTP $resp.status:$resp.statusLine\n"
            error = """{
        "Message":"${message}",
       "Target":"${target}",
    "Status":"Error: $resp.status:$resp.statusLine. Please make sure you are using a valid AT&T phone number (XXXYYYZZZZ)."
}"""
            return ["Content-Type":"application/json","Content":error]
        }
    }
} catch(e) {
    def stack = ExceptionUtils.getFullStackTrace(e)
    def errorMessage = """        {
        "Message":"${message}",
        "Target":"${target}",
        "Status":"${e.message}, ${stack}"
    }
"""
    logger.info "FAILED: Exception occurred: ${e.message}, ${stack}"
    return ["Content-Type":"application/json","Content":errorMessage]
}
